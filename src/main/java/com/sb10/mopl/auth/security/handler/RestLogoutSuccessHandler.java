package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestLogoutSuccessHandler implements LogoutSuccessHandler {

  private final AuthErrorResponseWriter responseWriter;
  private final JwtProperties jwtProperties;

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    response.setDateHeader(HttpHeaders.EXPIRES, 0);

    if (authentication == null && !hasLogoutCredential(request)) {
      responseWriter.write(
          response, AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", "인증이 필요합니다."));
      return;
    }

    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  private boolean hasLogoutCredential(HttpServletRequest request) {
    return hasAuthorizationHeader(request) || hasRefreshTokenCookie(request);
  }

  private boolean hasAuthorizationHeader(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    return authorization != null && !authorization.isBlank();
  }

  private boolean hasRefreshTokenCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return false;
    }

    String refreshTokenCookieName = jwtProperties.refreshTokenCookie().name();
    return Arrays.stream(cookies)
        .anyMatch(cookie -> refreshTokenCookieName.equals(cookie.getName()));
  }
}
