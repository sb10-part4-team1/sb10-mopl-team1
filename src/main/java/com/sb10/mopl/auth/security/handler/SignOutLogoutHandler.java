package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignOutLogoutHandler implements LogoutHandler {

  private final AuthSessionService authSessionService;
  private final RefreshTokenCookieWriter refreshTokenCookieWriter;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      authSessionService.invalidateAllByUserId(user.id());
      refreshTokenCookieWriter.expireRefreshTokenCookie(response);
    }
  }
}
