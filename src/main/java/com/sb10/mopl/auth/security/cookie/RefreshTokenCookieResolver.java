package com.sb10.mopl.auth.security.cookie;

import com.sb10.mopl.auth.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieResolver {

  private final JwtProperties jwtProperties;

  public Optional<String> resolve(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    String refreshTokenCookieName = jwtProperties.refreshTokenCookie().name();
    return Arrays.stream(cookies)
        .filter(cookie -> refreshTokenCookieName.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  public boolean exists(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
