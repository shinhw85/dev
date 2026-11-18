package smartsuite.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.GenericFilterBean;

import com.beust.jcommander.internal.Lists;

import jakarta.servlet.Filter;
import smartsuite.config.data.SecurityConst;
import smartsuite.config.security.CryptoConfiguration;
import smartsuite.config.security.MethodSecurityConfiguration;
import smartsuite.config.security.SamlConfiguration;
import smartsuite.config.security.SessionConfiguration;
import smartsuite.config.security.filter.DefaultFilterConfiguration;
import smartsuite.config.security.filter.OtherFilterConfiguration;
import smartsuite.config.security.filter.SkipRequestFilter;
import smartsuite.config.security.filter.SupplierFilterConfiguration;
import smartsuite.security.authentication.AuthenticationUrlRequestManager;
import smartsuite.security.authentication.DefaultAuthenticationSuccessHandler;
import smartsuite.security.authentication.SsoAuthenticationFailureHandler;
import smartsuite.security.authentication.SsoAuthenticationSuccessHandler;
import smartsuite.security.authentication.SupplierAuthenticationSuccessHandler;
import smartsuite.security.spring.web.util.matcher.DefaultRequiresCsrfMatcher;
import smartsuite.security.spring.web.util.matcher.NegatedOrRequestMatcher;
import smartsuite.security.web.filter.advanced.XssRequestFilter;

@Configuration
@EnableWebSecurity
@SuppressWarnings ({ "rawtypes", "unchecked" })
//@ImportResource(value = { "classpath:smartsuite/security-context.xml" })
@Import({CryptoConfiguration.class, SessionConfiguration.class, DefaultFilterConfiguration.class, OtherFilterConfiguration.class, SupplierFilterConfiguration.class, MethodSecurityConfiguration.class, SamlConfiguration.class})
public class SecurityConfiguration {

	@Value("#{smartsuiteProperties['smartsuite.jsessionid']}")
	private String jsessionid = "JSESSIONID";
	
	@Value("#{securityProperties['security.csp.default-policy']}")
	private String defaultPolicy;
	
	@Value("#{securityProperties['security.csp.no-session-policy']}")
	private String noSessionPolicy;
	
	@Value("#{securityProperties['security.csp.guarantee-policy']}")
	private String guaranteePolicy;
	
	@Value("#{securityProperties['security.csp.docusign-policy']}")
	private String docusignPolicy;

	@Value("#{securityProperties['security.csp.no-session-vendor-decrypt-policy']}")
	private String noSessionVendorDecryptPolicy;

	@Value("#{securityProperties['security.noSession.vendor.decrypt.requestList'].split(',')}")
	List<String> noSessionVendorUserDecryptManageRequest;
	
	@Value("#{securityProperties['security.noSession.requestList'].split(',')}")
	List<String> noSessionManageRequest;
	
	@Value("#{securityProperties['security.guarantee.requestList'].split(',')}")
	List<String> guaranteeManageRequest;
	
	@Value("#{securityProperties['security.docusign.requestList'].split(',')}")
	List<String> docusignManageRequest;

	@Value("#{securityProperties['security.ouath.requestList'].split(',')}")
	List<String> oauthManageRequest;
	
	@Value("#{securityProperties['security.csrf-exclude.requestList'].split(',')}")
	List<String> csrfExcludeRequestList;
	
	
	/**
	 * 와일드카드 사용금지
	 * 세션 없이 접속 가능한 Filter
	 * 대부분 SpLogin 내에 존재하는 호출자들에 대해서 사용
	 * @return
	 */
	private List<RequestMatcher> noSessionVendorUserDecryptRequestMatchers(){
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		for (String antPathRequestMatcher : noSessionVendorUserDecryptManageRequest){
			antPathRequestMatchers.add(new AntPathRequestMatcher(antPathRequestMatcher));
		}
		return antPathRequestMatchers;
	}

	/**
	 * 와일드카드 사용금지
	 * 세션 없이 접속 가능한 Filter
	 * 대부분 SpLogin 내에 존재하는 호출자들에 대해서 사용
	 * @return
	 */
	@Bean("noSessionVendorUserDecryptSessionManageRequest")
	public RequestMatcher noSessionVendorUserDecryptSessionManageRequest() {
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		antPathRequestMatchers.addAll(this.noSessionVendorUserDecryptRequestMatchers());
		return new OrRequestMatcher(antPathRequestMatchers);
	}

	/**
	 * 와일드카드 사용금지
	 * 세션 없이 접속 가능한 Filter
	 * @return
	 */
	private List<RequestMatcher> noSessionManageRequestMatchers(){
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		for (String antPathRequestMatcher : noSessionManageRequest){
			antPathRequestMatchers.add(new AntPathRequestMatcher(antPathRequestMatcher));
		}
		return antPathRequestMatchers;
	}

	/**
	 * 와일드카드 사용금지
	 * 세션 없이 접속 가능한 Filter
	 * @return
	 */
	@Bean("noSessionManageRequest")
	public RequestMatcher noSessionManageRequest() {
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		antPathRequestMatchers.addAll(this.noSessionManageRequestMatchers());
		return new OrRequestMatcher(antPathRequestMatchers);
	}
	
	
	private List<RequestMatcher> guaranteeManageRequestMatchers(){
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		for (String antPathRequestMatcher : guaranteeManageRequest){
			antPathRequestMatchers.add(new AntPathRequestMatcher(antPathRequestMatcher));
		}
		return antPathRequestMatchers;
	}

	/**
	 * 서울보증보험 온라인 연계시 사용
	 * @return
	 */
	@Bean("guaranteeManageRequest")
	public RequestMatcher guaranteeManageRequest() {
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		antPathRequestMatchers.addAll(this.guaranteeManageRequestMatchers());
		return new OrRequestMatcher(antPathRequestMatchers);
	}
	
	
	private List<RequestMatcher> docusignManageRequestMatchers(){
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		for (String antPathRequestMatcher : docusignManageRequest){
			antPathRequestMatchers.add(new AntPathRequestMatcher(antPathRequestMatcher));
		}
		return antPathRequestMatchers;
	}

	/**
	 * docusign embed 방식에서 사용
	 * @return
	 */
	@Bean("docusignManageRequest")
	public RequestMatcher docusignManageRequest() {
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		antPathRequestMatchers.addAll(this.docusignManageRequestMatchers());
		return new OrRequestMatcher(antPathRequestMatchers);
	}
	
	
	private List<RequestMatcher> oauthManageRequestMatchers(){
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		for (String antPathRequestMatcher : oauthManageRequest){
			antPathRequestMatchers.add(new AntPathRequestMatcher(antPathRequestMatcher));
		}
		return antPathRequestMatchers;
	}

	/**
	 * OAUTH2
	 * @return
	 */
	@Bean("oauthManageRequest")
	public RequestMatcher oauthManageRequest() {
		List<RequestMatcher> antPathRequestMatchers = Lists.newArrayList();
		antPathRequestMatchers.addAll(this.oauthManageRequestMatchers());
		return new NegatedOrRequestMatcher(antPathRequestMatchers);
	}


	/**
	 * csrf 필터 예외처리 url 와일드카드 사용금지
	 * SAML ACS(IdP가 POST 하는 경로)는 CSRF 토큰을 가질 수 없으므로 정확한 경로만 예외 추가
	 * @return
	 */
	@Bean("csrfExcludeRequest")
	public RequestMatcher csrfExcludeRequestMather() {
		List<RequestMatcher> excludeList = csrfExcludeRequestList.stream()
				.map(matcher -> new AntPathRequestMatcher(matcher))
				.collect(Collectors.toCollection(ArrayList<RequestMatcher>::new));
		excludeList.add(new AntPathRequestMatcher(SamlConfiguration.ACS_PATH));
		return new AndRequestMatcher(
				new NegatedOrRequestMatcher(excludeList),
				new DefaultRequiresCsrfMatcher() // Spring Security CSRF Matcher GET, HEAD, TRACE, OPTIONS
		);
	}

	/**
	 * SAML 관련 경로 (인증요청 / ACS / 메타데이터 / 로그아웃)
	 * XSS·복호화 필터 제외 및 permitAll 대상
	 */
	private RequestMatcher samlRequestMatcher() {
		return new OrRequestMatcher(
				new AntPathRequestMatcher("/saml2/**"),
				new AntPathRequestMatcher("/login/saml2/**"),
				new AntPathRequestMatcher("/logout/saml2/**"));
	}

	@Order(0)
	@Bean(name="noSessionFilterChain")
	public SecurityFilterChain noSessionFilterChain(HttpSecurity http,
													@Qualifier("delegatingAuthenticationEntryPoint") AuthenticationEntryPoint delegatingAuthenticationEntryPoint,
													@Qualifier("defaultAuthenticationManager") AuthenticationManager defaultAuthenticationManager,
													@Qualifier("tenantFilter") GenericFilterBean tenantFilter,
													@Qualifier("requestCache") HttpSessionRequestCache requestCache) throws Exception {

		http
				.securityMatcher(noSessionManageRequest())
				.httpBasic(basic -> basic
						.authenticationEntryPoint(delegatingAuthenticationEntryPoint)
						.and()
						.authenticationManager(defaultAuthenticationManager)
				)
				.csrf(csrf -> csrf.requireCsrfProtectionMatcher(csrfExcludeRequestMather()))
				.requestCache().requestCache(requestCache)
				.and()
				.headers(headers -> headers
						.contentSecurityPolicy(noSessionPolicy)
						.and()
						.frameOptions().sameOrigin()
				).addFilterBefore(tenantFilter, DisableEncodeUrlFilter.class);
		return http.build();
	}

	@Order(1)
	@Bean(name="noSessionVendorUserDecryptFilterChainCheck")
	public SecurityFilterChain noSessionVendorUserDecryptFilterChainCheck(HttpSecurity http,
			@Qualifier("delegatingAuthenticationEntryPoint") AuthenticationEntryPoint delegatingAuthenticationEntryPoint,
			@Qualifier("defaultAuthenticationManager") AuthenticationManager defaultAuthenticationManager,
			@Qualifier("tenantFilter") GenericFilterBean tenantFilter,
			@Qualifier("decrytRequestFilter")Filter decrytRequestFilter,
			@Qualifier("requestCache")HttpSessionRequestCache requestCache) throws Exception {

		http
			.securityMatcher(noSessionVendorUserDecryptSessionManageRequest())
			.httpBasic(basic -> basic
				.authenticationEntryPoint(delegatingAuthenticationEntryPoint)
				.and()
				.authenticationManager(defaultAuthenticationManager)
			)
			.csrf(csrf -> csrf.requireCsrfProtectionMatcher(csrfExcludeRequestMather()))
			.requestCache().requestCache(requestCache)
			.and()
			.headers(headers -> headers
				.contentSecurityPolicy(noSessionVendorDecryptPolicy)
				.and()
				.frameOptions().sameOrigin()
			).addFilterBefore(tenantFilter, DisableEncodeUrlFilter.class)
			.addFilterBefore(decrytRequestFilter, SecurityContextPersistenceFilter.class);
		return http.build();
	}

	// 서울보증보험 연계시 url 제외(csrf, session 제외)
	@Order(2)
	@Bean(name="guaranteeFilterChain")
	public SecurityFilterChain guaranteeFilterChain(HttpSecurity http, 
			@Qualifier("delegatingAuthenticationEntryPoint") AuthenticationEntryPoint delegatingAuthenticationEntryPoint,
			@Qualifier("defaultAuthenticationManager") AuthenticationManager defaultAuthenticationManager,
			@Qualifier("requestCache") HttpSessionRequestCache requestCache) throws Exception {
		
		http
		.securityMatcher(guaranteeManageRequest())
				.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(delegatingAuthenticationEntryPoint))
				.authenticationManager(defaultAuthenticationManager)
				.csrf(csrf -> csrf.requireCsrfProtectionMatcher(csrfExcludeRequestMather()))
		.requestCache().requestCache(requestCache)
		.and()
		.headers(headers -> headers
			.contentSecurityPolicy(guaranteePolicy)
		);
		return http.build();
	}

	@Order(3)
	@Bean
	@SuppressWarnings ({ "rawtypes", "unchecked" })
	public SecurityFilterChain docusignFilterChain(HttpSecurity http,
			AuthenticationEntryPoint delegatingAuthenticationEntryPoint,
			@Qualifier("defaultAuthenticationManager") AuthenticationManager defaultAuthenticationManager,
			HttpSessionRequestCache requestCache) throws Exception {

		http
			.securityMatcher(docusignManageRequest())
			.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(delegatingAuthenticationEntryPoint))
			.authenticationManager(defaultAuthenticationManager)
			.csrf(csrf -> csrf.disable()) //NOSONAR : DocuSign embed는 일반적으로 CSRF 불필요
			.requestCache().requestCache(requestCache)
			.and()
			.headers(headers -> headers
					.contentSecurityPolicy(docusignPolicy)
					.and()
					.frameOptions().disable() // SAMEORIGIN은 안 되므로 disable
			)
			.authorizeHttpRequests(req -> req
					.anyRequest().permitAll()
			);
		return http.build();
	}

	@Order(4)
	@Bean
	public SecurityFilterChain defaultFilterChain(HttpSecurity http,
			@Qualifier("delegatingAuthenticationEntryPoint") AuthenticationEntryPoint delegatingAuthenticationEntryPoint,
			@Qualifier("defaultAuthenticationManager") AuthenticationManager defaultAuthenticationManager,
			@Qualifier("requestCache") HttpSessionRequestCache requestCache,
			@Qualifier("authenticationSuccessHandler") DefaultAuthenticationSuccessHandler authenticationSuccessHandler,
			@Qualifier("authenticationFailureHandler") SimpleUrlAuthenticationFailureHandler authenticationFailureHandler,
			@Qualifier("logoutSuccessHandler") SimpleUrlLogoutSuccessHandler logoutSuccessHandler,
			@Qualifier("customAuthenticationProcessingFilters") CompositeFilter customAuthenticationProcessingFilters,
			@Qualifier("supplierAuthenticationSuccessHandler") SupplierAuthenticationSuccessHandler supplierAuthenticationSuccessHandler,
			@Qualifier("supplierAuthenticationFailureHandler") SimpleUrlAuthenticationFailureHandler supplierAuthenticationFailureHandler,
			@Qualifier("tenantFilter") GenericFilterBean tenantFilter,
			@Qualifier("sessionAuthenticationStrategy") CompositeSessionAuthenticationStrategy sessionAuthenticationStrategy,
			@Qualifier("concurrencyFilter") GenericFilterBean concurrencyFilter,
			@Qualifier("sessionInvalidateFilter") GenericFilterBean sessionInvalidateFilter,
			@Qualifier("decrytRequestFilter") Filter decrytRequestFilter,
			@Qualifier("xssRequestFilter") XssRequestFilter xssRequestFilter,
			// SAML
			RelyingPartyRegistrationRepository samlRegistrationRepository,
			@Qualifier("samlAuthenticationFailureHandler") SsoAuthenticationFailureHandler samlAuthenticationFailureHandler,
			@Qualifier("samlAuthenticationProvider") OpenSaml5AuthenticationProvider samlAuthenticationProvider
			) throws Exception {

		RequestMatcher samlRequestMatcher = samlRequestMatcher();

		http
			.securityMatcher(oauthManageRequest()) // OAUTH2
			.httpBasic(basic -> basic
				.authenticationEntryPoint(delegatingAuthenticationEntryPoint)
				.and()
				.authenticationManager(defaultAuthenticationManager)
			)		
			.authorizeHttpRequests(req -> req
				.requestMatchers(samlRequestMatcher).permitAll() // SAML 인증요청/ACS/메타데이터
				.requestMatchers(new AntPathRequestMatcher(AuthenticationUrlRequestManager.BUYER_LOGOUT_SUCCESS_URL), new AntPathRequestMatcher(AuthenticationUrlRequestManager.BUYER_LOGOUT_SUCCESS_URL_EN)).permitAll()
				.anyRequest().authenticated()
			)	
			.csrf(csrf -> csrf.requireCsrfProtectionMatcher(csrfExcludeRequestMather()))
			.headers(headers -> headers
				.contentSecurityPolicy(defaultPolicy)
				.and()
				.frameOptions().sameOrigin()
			)
			.requestCache().requestCache(requestCache)
			// TODO spring6
			.and()
			.securityContext(securityContext -> {
				securityContext.requireExplicitSave(false);
			})
			.formLogin(form -> form
				.loginPage(AuthenticationUrlRequestManager.KOREA_BUYER_LOGIN_PAGE)
				.loginProcessingUrl(AuthenticationUrlRequestManager.BUYER_LOGIN_PROCESS_PAGE)
				.usernameParameter(SecurityConst.USER_NAME_PARAMETER)
				.passwordParameter(SecurityConst.ENCRYPT_PARAMETER)
				.successHandler(authenticationSuccessHandler)
				.failureHandler(authenticationFailureHandler)
				.permitAll()
			)					
			.formLogin(form -> form
				.loginPage(AuthenticationUrlRequestManager.ENGLISH_BUYER_LOGIN_PAGE)
				.loginProcessingUrl(AuthenticationUrlRequestManager.BUYER_LOGIN_PROCESS_PAGE)
				.usernameParameter(SecurityConst.USER_NAME_PARAMETER)
				.passwordParameter(SecurityConst.ENCRYPT_PARAMETER)
				.successHandler(authenticationSuccessHandler)
				.failureHandler(authenticationFailureHandler)
			)
	
			.formLogin(form -> form
				.loginPage(AuthenticationUrlRequestManager.DEFAULT_SUPPLIER_LOGIN_PAGE)
				.loginProcessingUrl(AuthenticationUrlRequestManager.SUPPLIER_LOGIN_PROCESS_PAGE)
				.usernameParameter(SecurityConst.USER_NAME_PARAMETER)
				.passwordParameter(SecurityConst.ENCRYPT_PARAMETER)
				.successHandler(supplierAuthenticationSuccessHandler)
				.failureHandler(supplierAuthenticationFailureHandler)
				.permitAll()
			)
			.formLogin(form -> form
				.loginPage(AuthenticationUrlRequestManager.KOREA_SUPPLIER_LOGIN_PAGE)
				.loginProcessingUrl(AuthenticationUrlRequestManager.SUPPLIER_LOGIN_PROCESS_PAGE)
				.usernameParameter(SecurityConst.USER_NAME_PARAMETER)
				.passwordParameter(SecurityConst.ENCRYPT_PARAMETER)
				.successHandler(supplierAuthenticationSuccessHandler)
				.failureHandler(supplierAuthenticationFailureHandler)
				.permitAll()
			)
			.formLogin(form -> form
				.loginPage(AuthenticationUrlRequestManager.ENGLISH_SUPPLIER_LOGIN_PAGE)
				.loginProcessingUrl(AuthenticationUrlRequestManager.SUPPLIER_LOGIN_PROCESS_PAGE)
				.usernameParameter(SecurityConst.USER_NAME_PARAMETER)
				.passwordParameter(SecurityConst.ENCRYPT_PARAMETER)
				.successHandler(supplierAuthenticationSuccessHandler)
				.failureHandler(supplierAuthenticationFailureHandler)
				.permitAll()
			)
			// 403 발생시, 리다이렉트할 URL을 가장 마지막에 위치
			.formLogin(form -> form
				.loginPage(AuthenticationUrlRequestManager.DEFAULT_BUYER_LOGIN_PAGE)
				.loginProcessingUrl(AuthenticationUrlRequestManager.BUYER_LOGIN_PROCESS_PAGE)
				.usernameParameter(SecurityConst.USER_NAME_PARAMETER)
				.passwordParameter(SecurityConst.ENCRYPT_PARAMETER)
				.successHandler(authenticationSuccessHandler)
				.failureHandler(authenticationFailureHandler)
			)
			// SAML2 SSO (SP) : 진입 URL /saml2/authenticate/idp, ACS /login/saml2/sso/idp
			.saml2Login(saml -> saml
				.relyingPartyRegistrationRepository(samlRegistrationRepository)
				.authenticationManager(new ProviderManager(samlAuthenticationProvider))
				.successHandler(authenticationSuccessHandler)
				.failureHandler(samlAuthenticationFailureHandler)
			)
			// SP 메타데이터 노출 : /saml2/service-provider-metadata/idp
			.saml2Metadata(Customizer.withDefaults())
			.logout(logout -> logout
				.logoutUrl(AuthenticationUrlRequestManager.LOGOUT_PROCESS_PAGE)
				.logoutSuccessHandler(logoutSuccessHandler)
				.invalidateHttpSession(true)
				.deleteCookies(jsessionid)
			)
			.addFilterBefore(tenantFilter, DisableEncodeUrlFilter.class)
			// SAML 경로는 SAMLResponse 변형 방지를 위해 복호화/XSS 필터 제외 (익명 서브클래스: 필터 클래스별 순서 등록 충돌 방지)
			.addFilterBefore(new SkipRequestFilter(decrytRequestFilter, samlRequestMatcher) {}, SecurityContextPersistenceFilter.class)
			.addFilterAfter(new SkipRequestFilter(xssRequestFilter, samlRequestMatcher) {}, SecurityContextPersistenceFilter.class)
			.addFilterAt(concurrencyFilter, ConcurrentSessionFilter.class)
			.addFilterBefore(customAuthenticationProcessingFilters, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(sessionInvalidateFilter, SessionManagementFilter.class)
			.sessionManagement(session -> session.sessionAuthenticationStrategy(sessionAuthenticationStrategy))
			;
			
		return http.build();
	}
}
