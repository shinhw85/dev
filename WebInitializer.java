package smartsuite;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import smartsuite.license.servlet.SMARTSuite9LicenseServlet;
import smartsuite.log.spring.executelog.ExecutionLoggingFilter;
import smartsuite.web.RequestWrapperFilter;

@Order(1)
public class WebInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext container) throws ServletException {

		/**
		 * 1. Root Context
		 */
		AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
		// 1) web.xml 의 context-param 읽기
		String activeProfiles = container.getInitParameter("spring.profiles.active"); // "prod"
		if (activeProfiles != null && !activeProfiles.isBlank()) {
			rootContext.getEnvironment().setActiveProfiles(activeProfiles.split(","));
		}
		rootContext.register(RootContextConfiguration.class);
		rootContext.refresh();
		// listener
		container.addListener(new ContextLoaderListener(rootContext)); // spring 설정을 처리하는 리스너
		// container.addListener(SpringCacheBusterInitializeListener.class); // cachebuster 모듈 spring 기반에서 연동하기 위한 필터
		container.addListener(HttpSessionEventPublisher.class); // 중복 로그인 방지 기능을 위한 필터
		container.addListener(RequestContextListener.class); // Request 객체 참조하기 위한 리스너
		/**
		 * 2. filters (필터의 순서에 주의)
		 */
		// XSSFilter xssFilter = new XSSFilter();
		// container.addFilter("xssFilter", xssFilter).addMappingForServletNames(null, false, "dispatcherServlet");

		CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
		encodingFilter.setEncoding("UTF-8");
		container.addFilter("encodingFilter", encodingFilter).addMappingForUrlPatterns(null, false, "/*");


		DelegatingFilterProxy delegatingFilterProxy = new DelegatingFilterProxy();
		container.addFilter("springSecurityFilterChain", delegatingFilterProxy).addMappingForServletNames(null, false, "dispatcherServlet");

		ExecutionLoggingFilter executionLoggingFilter = new ExecutionLoggingFilter();
		container.addFilter("executionLoggingFilter", executionLoggingFilter).addMappingForUrlPatterns(null, false, "*.do");

		// RESTful api 수신 처리를 위해 필터 추가
//		ApiWrapperFilter apiWrapperFilter = new ApiWrapperFilter();
//		container.addFilter("apiWrapperFilter", apiWrapperFilter).addMappingForUrlPatterns(null, false, "/sone/api/*");
		container.addFilter("apiWrapperFilter", new DelegatingFilterProxy("apiWrapperFilter")).addMappingForUrlPatterns(null, false, "/sone/api/*");


		// 서울보증보험 연계 시 추가 필터  RequestWrapperFilter
		RequestWrapperFilter requestWrapperFilter = new RequestWrapperFilter();
		container.addFilter("requestWrapperFilter", requestWrapperFilter).addMappingForUrlPatterns(null, false, "/sgic/recv.do");

		// 모바일 mobileApiFilter
		// MobileApiFilter mobileApiFilter = new MobileApiFilter();
		// container.addFilter("requestWrapperFilter", mobileApiFilter).addMappingForUrlPatterns(null, false, "/mobileLogin.do");

		/**
		 * 3. Servlet Context
		 */
		// License Servlet (라이선스 인증 서블릿)
		ServletRegistration.Dynamic licenseServlet = container.addServlet("licenseServlet", new SMARTSuite9LicenseServlet());
		licenseServlet.setLoadOnStartup(1);
		licenseServlet.addMapping("/license.do");

		// Dispatcher Servlet
		// 방법1 (XML) - dispatcher-context.xml을 그대로 유지할 수 있다. 단, web.xml의
		// welcome-file-list, session-config 등은 MvcConfiguration 에서 처리해야 하므로 기능이 불완전하다.
//		XmlWebApplicationContext servletContext = new XmlWebApplicationContext();
//		servletContext.setConfigLocation("classpath:smartsuite/dispatcher-context.xml");
//		DispatcherServlet dispatcherServlet = new DispatcherServlet(servletContext);
		// 방법2 (Java Config)
		AnnotationConfigWebApplicationContext acw = new AnnotationConfigWebApplicationContext();
		acw.register(WebMvcConfiguration.class);
		DispatcherServlet dispatcherServlet = new DispatcherServlet(acw);

		ServletRegistration.Dynamic appServlet = container.addServlet("dispatcherServlet", dispatcherServlet);
		appServlet.setLoadOnStartup(2);
		appServlet.addMapping("/websocket-servlet/*", "/oauth2/*", "/api/*", "/rest/api/extractLibraries", "/websocket-servlet/*", "*.do", "/rest/api/*", "/app/*", "/sone/api/*");

		MultipartConfigElement multipartConfig = rootContext.getBean(MultipartConfigElement.class);
		appServlet.setMultipartConfig(multipartConfig);

		// CXF Servlet
        ServletRegistration.Dynamic cxfServlet = container.addServlet("cxfServlet", new CXFServlet());
        cxfServlet.setLoadOnStartup(3);
        cxfServlet.addMapping("/cxf/*");
        // CXF Servlet

		container.setAttribute("smartsuite.acw.context",acw);
	}
}
