package smartsuite.mybatis;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import smartsuite.app.crypto.EgCryptoService;

@Intercepts({
  @Signature(type = Executor.class, method = "update",
             args = {MappedStatement.class, Object.class}),
  @Signature(type = Executor.class, method = "query",
             args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(type = Executor.class, method = "query",
             args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                     CacheKey.class, BoundSql.class})
})
public class CryptoInterceptor implements Interceptor {

  private final EgCryptoService client;
  private final CryptoTargets targets;

  public CryptoInterceptor(EgCryptoService client, CryptoTargets targets) {
	  this.client = client;
	  this.targets = targets;
  }

  @Override
  public Object intercept(Invocation inv) throws Throwable {
    Object[] args = inv.getArgs();
    MappedStatement ms = (MappedStatement) args[0];
    Set<String> keys = targets.find(ms.getId());
    if (keys == null) return inv.proceed();        // 대상 아님 → 그대로

    
    // 1) 파라미터 암호화 (원본 Map 오염 방지를 위해 복사본 사용)
    args[1] = client.enc((String) args[1]); //encryptParam(args[1], keys);

    Object result = inv.proceed();

    // 2) SELECT 결과 복호화
    if (result instanceof List<?> list && !list.isEmpty()) {
    	client.dec((List<String>) list); //decryptList(list, keys);                     // 값 모아서 String[] 일괄 호출
    }
    return result;
  }
  
}
