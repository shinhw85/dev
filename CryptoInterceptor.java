package smartsuite.mybatis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 대상 statement의 지정 키만 EG 암복호화를 적용하는 MyBatis 플러그인.
 *
 * - insert/update/delete/select 실행 전: 파라미터의 대상 키 값을 암호화
 *   (호출한 쪽 객체를 오염시키지 않도록 실행 후 원래 값으로 복원)
 * - select 실행 후: 결과 목록의 대상 키 값을 복호화 (건수만큼 모아 배열로 1회 호출)
 *
 * 지원 범위: String 값. 파라미터는 Map / DTO / 단일 String(_parameter).
 * 미지원: ResultHandler 방식 조회, Cursor 조회, List/배열 파라미터(IN 조건) 내부 값.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class CryptoInterceptor implements Interceptor {
	static final Logger LOG = LoggerFactory.getLogger(CryptoInterceptor.class);
	
    /** 파라미터가 String 하나뿐인 쿼리(예: selectByTel(String tel))의 대상 키 이름 */
    public static final String SINGLE_PARAM = "_parameter";
    public final String paramPrefix;    
    private final CryptoClient client;
    private final CryptoTargets targets;

    public CryptoInterceptor(CryptoClient client, CryptoTargets targets) {
        this(client, targets, "");
    }
    
    public CryptoInterceptor(CryptoClient client, CryptoTargets targets, String paramPrefix) {
    	this.client = client;
    	this.targets = targets;
    	this.paramPrefix = paramPrefix == null ? "" : paramPrefix;    	
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];

        CryptoTargets.Target target = targets.find(ms.getId());
        if (target == null) {
            return invocation.proceed();               // 대상 아님 → 그대로
        }

        Object p = args[1];
        LOG.info("[crypto] paramClass={} keys={}", p == null ? null : p.getClass().getName(), p instanceof Map<?, ?> m ? m.keySet() : "(not Map)");
        ms.getBoundSql(p).getParameterMappings() .forEach(pm -> LOG.info("[crypto] SQL param property={}", pm.getProperty()));
        
        LOG.info("[crypto] id={} cmd={} paramKey={} resultKey={}", ms.getId(), ms.getSqlCommandType(), target.paramKeys(), target.resultKeys());
        
        Object originalParam = args[1];
        List<Slot> encrypted = new ArrayList<>();
        try {
            args[1] = encryptParam(ms, originalParam, target.paramKeys(), encrypted);

            Object result = invocation.proceed();

            if (ms.getSqlCommandType() == SqlCommandType.SELECT && result instanceof List<?> rows) {
                decryptRows(ms, rows, target.resultKeys());
            }
            return result;
        } finally {
            args[1] = originalParam;
            for (Slot s : encrypted) {                  // 호출한 쪽 객체를 평문으로 복원
                s.setter.accept(s.value);
            }
        }
    }

    // ---------------------------------------------------------------- 파라미터 암호화

    private Object encryptParam(MappedStatement ms, Object param, Set<String> keys, List<Slot> restoreList) {
        if (param == null || keys.isEmpty()) {
            return param;
        }

        // 단일 String 파라미터
        if (param instanceof String s) {
            return (keys.contains(SINGLE_PARAM) && !s.isEmpty()) ? client.encrypt(s) : param;
        }
        // Number, Date 등 단순 타입은 대상 아님
        if (ms.getConfiguration().getTypeHandlerRegistry().hasTypeHandler(param.getClass())) {
            return param;
        }

        MetaObject mo = SystemMetaObject.forObject(param);
        List<Slot> found = new ArrayList<>();
        for (String key : keys) {
            if (SINGLE_PARAM.equals(key)) continue;
            String prop = resolveParamProperty(mo, key);
            if (prop == null) continue;

            Object v = mo.getValue(prop);
            if (v instanceof String s && !s.isEmpty()) {
                found.add(new Slot(s, nv -> mo.setValue(prop, nv)));
            }
        }
        if (found.isEmpty()) {
            return param;
        }

        restoreList.addAll(found);                      // 도중 실패해도 복원되도록 먼저 등록
        convert(found, true);
        return param;
    }

    /** Map: 키 그대로(중첩 "dto.field" 허용), DTO: 프로퍼티명(대소문자/언더스코어 무시) */
    private String resolveParamProperty(MetaObject mo, String key) {
    	if(!paramPrefix.isEmpty()) {
    		String prefixed = resolveRow(mo, paramPrefix+key);
    		if(prefixed != null) return prefixed;
    	}
    	return resolveRow(mo, key);
    }
    
    private String resolveRow(MetaObject mo, String key) {
    	try {
    		Object obj = mo.getOriginalObject();
    		if (obj instanceof Map<?, ?> m) {
    			if (m.containsKey(key)) return key;
    			if (key.indexOf('.') > 0 && mo.hasGetter(key) && mo.hasSetter(key)) return key;
    			return null;
    		}
    		String prop = mo.findProperty(key, true);
    		return (prop != null && mo.hasGetter(prop) && mo.hasSetter(prop)) ? prop : null;
    	} catch (RuntimeException e) {
    		return null;                                // 없는 키 접근 시 예외 → 대상 아님
    	}
    }

    // ---------------------------------------------------------------- 결과 복호화

    @SuppressWarnings("unchecked")
    private void decryptRows(MappedStatement ms, List<?> rows, Set<String> keys) {
        if (rows.isEmpty() || keys.isEmpty()) {
            return;
        }
        var registry = ms.getConfiguration().getTypeHandlerRegistry();
        List<Slot> found = new ArrayList<>();

        for (Object row : rows) {
            if (row == null) continue;

            if (row instanceof Map<?, ?> map) {
                Map<Object, Object> m = (Map<Object, Object>) map;
                for (Map.Entry<Object, Object> e : m.entrySet()) {
                    if (e.getKey() instanceof String k
                            && e.getValue() instanceof String v
                            && !v.isEmpty()
                            && keys.contains(k.toLowerCase(java.util.Locale.ROOT))) {
                        Object mapKey = e.getKey();
                        found.add(new Slot(v, nv -> m.put(mapKey, nv)));
                    }
                }
            } else if (!registry.hasTypeHandler(row.getClass())) {     // String/Long 등 단순 타입 제외
                MetaObject mo = SystemMetaObject.forObject(row);
                for (String key : keys) {
                    String prop = mo.findProperty(key, true);          // user_nm → userNm
                    if (prop == null || !mo.hasGetter(prop) || !mo.hasSetter(prop)) continue;

                    Object v = mo.getValue(prop);
                    if (v instanceof String s && !s.isEmpty()) {
                        found.add(new Slot(s, nv -> mo.setValue(prop, nv)));
                    }
                }
            }
        }

        if (!found.isEmpty()) {
            convert(found, false);
        }
    }

    // ---------------------------------------------------------------- 공통

    /** 모은 값을 배열로 1회 호출해 같은 순서로 되돌려 넣는다. */
    private void convert(List<Slot> slots, boolean encrypt) {
    	LOG.info("[crypto] {} {}건", encrypt ? "ENC" : "DEC", slots.size());
    	
        String[] in = new String[slots.size()];
        for (int i = 0; i < in.length; i++) {
            in[i] = slots.get(i).value;
        }

        String[] out = encrypt ? client.encrypt(in) : client.decrypt(in);
        if (out == null || out.length != in.length) {
            throw new IllegalStateException("암복호화 응답 건수 불일치: 요청=" + in.length
                    + ", 응답=" + (out == null ? "null" : out.length));
        }
        for (int i = 0; i < out.length; i++) {
            slots.get(i).setter.accept(out[i]);
        }
    }

    /** 값 하나와 그 값을 다시 써 넣는 방법 */
    private static final class Slot {
        final String value;
        final Consumer<String> setter;

        Slot(String value, Consumer<String> setter) {
            this.value = value;
            this.setter = setter;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 사용 안 함
    }
}
