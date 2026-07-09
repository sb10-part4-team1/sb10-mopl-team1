package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieResolver;
import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
  private final RefreshTokenCookieResolver refreshTokenCookieResolver;
  private final RefreshTokenCookieWriter refreshTokenCookieWriter;

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

    try {
      refreshTokenCookieResolver.resolve(request).ifPresent(refreshTokenService::revoke);
    } catch (RuntimeException exception) {
      log.error("Failed to revoke refresh token on logout.", exception);
    } finally {
      refreshTokenCookieWriter.expireRefreshTokenCookie(response);
    }
  }
}
