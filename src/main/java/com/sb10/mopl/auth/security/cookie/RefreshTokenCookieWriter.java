package com.sb10.mopl.auth.security.cookie;

import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.auth.service.RefreshTokenService.IssuedRefreshToken;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieWriter {

  private final JwtProperties jwtProperties;

  public void addRefreshTokenCookie(
      HttpServletResponse response, IssuedRefreshToken issuedRefreshToken) {
    JwtProperties.RefreshTokenCookie cookieProperties = jwtProperties.refreshTokenCookie();
    ResponseCookie cookie =
        ResponseCookie.from(cookieProperties.name(), issuedRefreshToken.rawToken())
            .httpOnly(cookieProperties.httpOnly())
            .secure(cookieProperties.secure())
            .sameSite(cookieProperties.sameSite())
            .path(cookieProperties.path())
            .maxAge(jwtProperties.refreshTokenExpiration())
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void expireRefreshTokenCookie(HttpServletResponse response) {
    JwtProperties.RefreshTokenCookie cookieProperties = jwtProperties.refreshTokenCookie();
    ResponseCookie cookie =
        ResponseCookie.from(cookieProperties.name(), "")
            .httpOnly(cookieProperties.httpOnly())
            .secure(cookieProperties.secure())
            .sameSite(cookieProperties.sameSite())
            .path(cookieProperties.path())
            .maxAge(Duration.ZERO)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
