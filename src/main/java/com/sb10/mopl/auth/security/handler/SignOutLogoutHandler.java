package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.auth.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignOutLogoutHandler implements LogoutHandler {

  private final AuthSessionService authSessionService;
  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookieWriter refreshTokenCookieWriter;
  private final JwtProperties jwtProperties;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      try {
        authSessionService.invalidateAllByUserId(user.id());
      } catch (RuntimeException exception) {
        log.error(
            "Failed to invalidate authentication session on logout. userId={}",
            user.id(),
            exception);
      }
    }

    refreshTokenService.revoke(resolveRefreshToken(request));
    refreshTokenCookieWriter.expireRefreshTokenCookie(response);
  }

  private String resolveRefreshToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    String refreshTokenCookieName = jwtProperties.refreshTokenCookie().name();
    return Arrays.stream(cookies)
        .filter(cookie -> refreshTokenCookieName.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
