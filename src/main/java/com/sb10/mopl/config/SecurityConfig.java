package com.sb10.mopl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Spring Security 설정을 담당하는 클래스입니다. 개발 및 테스트 단계에서 임시로 모든 API 경로에 대한 접근 권한을 허용합니다. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * HTTP 보안 필터 체인을 설정하여 모든 요청을 허용하고 CSRF를 비활성화합니다.
   *
   * @param http HttpSecurity 설정용 객체
   * @return 설정이 반영된 SecurityFilterChain 객체
   * @throws Exception 보안 구성 중 발생하는 예외
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        // H2-Console 사용을 위한 헤더 설정. 추후 제거 예정
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
