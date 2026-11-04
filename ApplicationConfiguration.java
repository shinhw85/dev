// ===== ApplicationConfiguration.java 에서 기존 cryptoTargets / cryptoInterceptor 빈을 아래로 교체 =====
// 추가 import:
//   import org.springframework.core.io.ClassPathResource;
//   import org.springframework.core.io.support.EncodedResource;
//   import org.springframework.core.io.support.PropertiesLoaderUtils;
//   (org.springframework.context.annotation.* 는 이미 import 되어 있어 @Lazy 포함)
//   import smartsuite.mybatis.CryptoClient;
//   import smartsuite.mybatis.EgCryptoClient;

    // EgCryptoService → ApiSenderService 가 MyBatis를 쓰면 순환 참조가 생기므로 @Lazy
    @Bean
    public CryptoClient cryptoClient(@Lazy EgCryptoService eg) {
        return new EgCryptoClient(eg);
    }

    @Bean
    public CryptoTargets cryptoTargets() throws IOException {
        Properties p = PropertiesLoaderUtils.loadProperties(
                new EncodedResource(new ClassPathResource("crypto-targets.properties"), "UTF-8"));
        return new CryptoTargets(p);
    }

    @Bean
    public CryptoInterceptor cryptoInterceptor(CryptoClient client, CryptoTargets targets) {
        return new CryptoInterceptor(client, targets);
    }

// ===== MybatisConfiguration.java 의 SqlSessionFactoryBean 설정 =====
//   fb.setPlugins(cryptoInterceptor);   // 메서드 파라미터로 CryptoInterceptor 주입받아 사용
//   (선택) SqlSessionFactory factory = fb.getObject();
//          cryptoTargets.validate(factory.getConfiguration());   // 오타 id 경고