package com.sb10.mopl.config;

import com.sb10.mopl.auth.security.filter.EmailPasswordAuthenticationFilter;
import com.sb10.mopl.auth.security.handler.AuthErrorResponseWriter;
import com.sb10.mopl.auth.security.jwt.JwtAuthenticationFilter;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.user.entity.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

  // 공개/관리자 규칙에 걸리지 않은 API 요청을 마지막에 한 번 더 닫기 위한 백엔드 API 범위
  private static final RequestMatcher API_ENDPOINT_MATCHER = pathMatcher("/api/**");

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
    pathMatcher("/h2-console/**"),
    pathMatcher("/api-docs/**"),
    pathMatcher("/swagger-ui/**"),
    pathMatcher("/swagger-ui.html"),
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
    methodAndPathMatcher(HttpMethod.PATCH, "/api/users/*/locked")
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      EmailPasswordAuthenticationFilter emailPasswordAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint)
      throws Exception {
    // 이번 PR 범위에서는 CSRF 발급/검증을 아직 구현하지 않음. 추후 구현 예정
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
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
                exceptionHandling.authenticationEntryPoint(authenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAt(emailPasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtProvider jwtProvider, AuthErrorResponseWriter authErrorResponseWriter) {
    return new JwtAuthenticationFilter(
        jwtProvider, authErrorResponseWriter, PUBLIC_ENDPOINT_MATCHERS);
  }

  @Bean
  public EmailPasswordAuthenticationFilter emailPasswordAuthenticationFilter(
      AuthenticationManager authenticationManager,
      AuthenticationSuccessHandler authenticationSuccessHandler,
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
      AuthenticationProvider authenticationProvider) {
    return new ProviderManager(authenticationProvider);
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
}
