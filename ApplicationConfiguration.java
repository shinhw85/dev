package smartsuite.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import smartsuite.app.crypto.EgCryptoService;
import smartsuite.config.application.AttachmentConfiguration;
import smartsuite.config.application.CacheConfiguration;
import smartsuite.config.application.MailConfiguration;
import smartsuite.config.application.ModuleConfiguration;
import smartsuite.config.application.MybatisConfiguration;
import smartsuite.mybatis.CryptoClient;
import smartsuite.mybatis.CryptoInterceptor;
import smartsuite.mybatis.CryptoTargets;
import smartsuite.mybatis.EgCryptoClient;
import smartsuite.security.captcha.CaptchaGenerator;

@Configuration
@Import({
	MybatisConfiguration.class,
	AttachmentConfiguration.class,
	ModuleConfiguration.class,
	CacheConfiguration.class,
    MailConfiguration.class,
    WebServiceConfiguration.class,
	MailAsyncConfiguration.class
})
@ComponentScan(
	basePackages = { "smartsuite.config.module" },
        useDefaultFilters = false,
	includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class)
)
//@ImportResource(value = { "classpath*:smartsuite/module/*-context.xml" })
@ComponentScan(
    basePackages = {
        "smartsuite.*.web",
        "smartsuite.security",
        "smartsuite.tenancy",
        "smartsuite.app",
        "smartsuite.mybatis",
        "smartsuite.config.workplace",
        "smartsuite5.*.web",
        "smartsuite.upload.auth"
    },
    useDefaultFilters = false,
    lazyInit = true,
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class)
)
public class ApplicationConfiguration {

	// moduleManager
	@Bean
	public MethodInvokingFactoryBean methodInvokingFactoryBean() {
		MethodInvokingFactoryBean factoryBean = new MethodInvokingFactoryBean();
		factoryBean.setStaticMethod("smartsuite.module.ModuleManager.setLocalProperties");
		factoryBean.setArguments("classpath*:smartsuite/module/*.properties");
		return factoryBean;
	}

	// objectMapper
    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // captcha 인증
    @Bean("captchaGenerator")
    public CaptchaGenerator captchaGenerator() {
        return new CaptchaGenerator();
    }

    // restTemplate
    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Gson
    @Bean("gsonBuilder")
    public GsonBuilder gsonBuilder() {
        return new GsonBuilder().disableHtmlEscaping();
    }

    @Bean("gson")
    public Gson gson() {
        return gsonBuilder().create();
    }

    @Bean
    public CryptoClient cryptoClient(@Lazy EgCryptoService eg) {
        return new EgCryptoClient(eg);
    }
    
    @Bean
    public CryptoTargets cryptoTargets() throws IOException {
        Properties p = PropertiesLoaderUtils.loadProperties(
                new EncodedResource(new ClassPathResource("smartsuite"+ File.separator+"properties"+ File.separator + "encrypt"+ File.separator +"crypto-targets.properties"), "UTF-8"));
        return new CryptoTargets(p);
    }

    @Bean
    public CryptoInterceptor cryptoInterceptor(CryptoClient client, CryptoTargets targets) {
        return new CryptoInterceptor(client, targets, "p.");
    }
}
