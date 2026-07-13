package com.sb10.mopl.config;

import com.sb10.mopl.auth.security.csrf.SpaCsrfTokenRequestHandler;
import com.sb10.mopl.auth.security.filter.EmailPasswordAuthenticationFilter;
import com.sb10.mopl.auth.security.handler.AuthErrorResponseWriter;
import com.sb10.mopl.auth.security.handler.Oauth2LoginFailureHandler;
import com.sb10.mopl.auth.security.handler.Oauth2LoginSuccessHandler;
import com.sb10.mopl.auth.security.jwt.JwtAuthenticationFilter;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.provider.TemporaryPasswordAuthenticationProvider;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.user.entity.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 컨트롤러 내 @PreAuthorize 메서드 보안 활성화
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

  // 공개/관리자 규칙에 걸리지 않은 API 요청을 마지막에 한 번 더 닫기 위한 백엔드 API 범위
  private static final RequestMatcher API_ENDPOINT_MATCHER = pathMatcher("/api/**");
  // 인증 실패 후에도 클라이언트 로그아웃 정리를 완료해야 하는 경로
  private static final RequestMatcher[] CONTINUE_ON_AUTHENTICATION_FAILURE_MATCHERS = {
    methodAndPathMatcher(HttpMethod.POST, "/api/auth/sign-out")
  };

  // 로그인하지 않은 사용자가 접근할 수 있어야 하는 경로 목록
  private static final RequestMatcher[] PUBLIC_ENDPOINT_MATCHERS = {
    pathMatcher("/"),
    pathMatcher("/index.html"),
    pathMatcher("/favicon.svg"),
    pathMatcher("/assets/**"),
    pathMatcher("/uploads/**"),
    pathMatcher("/error"),
    pathMatcher("/oauth2/**"),
    pathMatcher("/login/oauth2/**"),
    pathMatcher("/h2-console/**"), // FIXME: 나중에 지워야 할 부분,
    pathMatcher("/api-docs/**"),
    pathMatcher("/swagger-ui/**"),
    pathMatcher("/swagger-ui.html"),
    pathMatcher("/api/test/batch/**"), // FIXME: 나중에 지워야 할 부분,
    methodAndPathMatcher(HttpMethod.OPTIONS, "/**"),
    methodAndPathMatcher(HttpMethod.POST, "/api/users"),
    methodAndPathMatcher(HttpMethod.POST, "/api/auth/sign-in"),
    methodAndPathMatcher(HttpMethod.POST, "/api/auth/reset-password"),
    methodAndPathMatcher(HttpMethod.POST, "/api/auth/refresh"),
    methodAndPathMatcher(HttpMethod.GET, "/api/auth/csrf-token")
  };

  // 관리자 권한이 필요한 사용자 관리 API 목록
  private static final RequestMatcher[] ADMIN_ENDPOINT_MATCHERS = {
    methodAndPathMatcher(HttpMethod.GET, "/api/users"),
    methodAndPathMatcher(HttpMethod.PATCH, "/api/users/*/role"),
    methodAndPathMatcher(HttpMethod.PATCH, "/api/users/*/locked"),
    pathMatcher("/api/admin/batch/**"), // 관리자 배치 제어 권한 제한
    methodAndPathMatcher(HttpMethod.POST, "/api/contents/**"), // 콘텐츠 등록(POST) 권한 제한
    methodAndPathMatcher(HttpMethod.PATCH, "/api/contents/**"), // 콘텐츠 수정(PATCH) 권한 제한
    methodAndPathMatcher(HttpMethod.DELETE, "/api/contents/**") // 콘텐츠 삭제(DELETE) 권한 제한
  };

  private static RequestMatcher pathMatcher(String pattern) {
    return request -> PATH_MATCHER.match(pattern, path(request));
  }

  private static RequestMatcher methodAndPathMatcher(HttpMethod method, String pattern) {
    return request ->
        method.matches(request.getMethod()) && PATH_MATCHER.match(pattern, path(request));
  }

  private static String path(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath == null || contextPath.isBlank()) {
      return requestUri;
    }
    return requestUri.substring(contextPath.length());
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      EmailPasswordAuthenticationFilter emailPasswordAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler,
      LogoutHandler signOutLogoutHandler,
      LogoutSuccessHandler logoutSuccessHandler,
      ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
      Oauth2LoginSuccessHandler oauth2LoginSuccessHandler,
      Oauth2LoginFailureHandler oauth2LoginFailureHandler)
      throws Exception {
    boolean oauth2LoginEnabled = clientRegistrationRepository.getIfAvailable() != null;

    http.csrf(
            csrf ->
                csrf.ignoringRequestMatchers(
                        "/h2-console/**", "/api/test/batch/**") // FIXME: 나중에 지워야 할 부분,
                    .csrfTokenRepository(csrfTokenRepository())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session ->
                session.sessionCreationPolicy(
                    oauth2LoginEnabled
                        ? SessionCreationPolicy.IF_REQUIRED
                        : SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/sign-out")
                    .addLogoutHandler(signOutLogoutHandler)
                    .logoutSuccessHandler(logoutSuccessHandler)
                    .clearAuthentication(true)
                    .invalidateHttpSession(false))
        // H2-Console 사용을 위한 헤더 설정. 추후 제거 예정
        .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
        .authorizeHttpRequests(
            auth ->
                auth
                    // 회원가입, 로그인, OAuth2, 문서/개발 도구, 정적 리소스 같은 공개 경로 허용
                    .requestMatchers(PUBLIC_ENDPOINT_MATCHERS)
                    .permitAll()
                    // 사용자 목록 조회, 권한 변경, 계정 잠금 같은 사용자 관리 API는 관리자만 허용
                    .requestMatchers(ADMIN_ENDPOINT_MATCHERS)
                    .hasAuthority(UserRole.ADMIN.authorityName())
                    // 공개 경로와 관리자 경로에 포함되지 않은 모든 백엔드 API는 로그인한 사용자만 허용
                    .requestMatchers(API_ENDPOINT_MATCHER)
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    // 인증 정보가 없거나 유효하지 않은 요청은 401 응답으로 처리
                    .authenticationEntryPoint(authenticationEntryPoint)
                    // 인증은 되었지만 필요한 권한이 부족한 요청은 403 응답으로 처리
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, LogoutFilter.class)
        .addFilterAt(emailPasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    if (oauth2LoginEnabled) {
      http.oauth2Login(
          oauth2 ->
              oauth2
                  .successHandler(oauth2LoginSuccessHandler)
                  .failureHandler(oauth2LoginFailureHandler));
    }

    return http.build();
  }

  @Bean
  public CsrfTokenRepository csrfTokenRepository() {
    CookieCsrfTokenRepository cookieCsrfTokenRepository =
        CookieCsrfTokenRepository.withHttpOnlyFalse();
    cookieCsrfTokenRepository.setCookieName("XSRF-TOKEN");
    cookieCsrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
    cookieCsrfTokenRepository.setCookiePath("/");
    return cookieCsrfTokenRepository;
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtProvider jwtProvider,
      JwtSessionService jwtSessionService,
      AuthErrorResponseWriter authErrorResponseWriter) {
    return new JwtAuthenticationFilter(
        jwtProvider,
        jwtSessionService,
        authErrorResponseWriter,
        PUBLIC_ENDPOINT_MATCHERS,
        CONTINUE_ON_AUTHENTICATION_FAILURE_MATCHERS);
  }

  @Bean
  public EmailPasswordAuthenticationFilter emailPasswordAuthenticationFilter(
      AuthenticationManager authenticationManager,
      @Qualifier("jwtAuthenticationSuccessHandler")
          AuthenticationSuccessHandler authenticationSuccessHandler,
      @Qualifier("jsonAuthenticationFailureHandler")
          AuthenticationFailureHandler authenticationFailureHandler,
      Validator validator) {
    return new EmailPasswordAuthenticationFilter(
        authenticationManager,
        authenticationSuccessHandler,
        authenticationFailureHandler,
        validator);
  }

  @Bean
  public AuthenticationManager authenticationManager(
      @Qualifier("authenticationProvider") AuthenticationProvider passwordAuthenticationProvider,
      TemporaryPasswordAuthenticationProvider temporaryPasswordAuthenticationProvider) {
    return new ProviderManager(
        List.of(passwordAuthenticationProvider, temporaryPasswordAuthenticationProvider));
  }

  @Bean
  public AuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider authenticationProvider =
        new DaoAuthenticationProvider(userDetailsService);
    authenticationProvider.setPasswordEncoder(passwordEncoder);
    return authenticationProvider;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
