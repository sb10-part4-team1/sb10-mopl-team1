package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.auth.entity.SocialProvider;
import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.oauth.Oauth2UserProfile;
import com.sb10.mopl.auth.security.oauth.Oauth2UserProfileResolverComposite;
import com.sb10.mopl.auth.service.AuthTokenService;
import com.sb10.mopl.auth.service.AuthTokenService.IssuedToken;
import com.sb10.mopl.auth.service.SocialLoginService;
import com.sb10.mopl.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Oauth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final Oauth2UserProfileResolverComposite profileResolver;
  private final SocialLoginService socialLoginService;
  private final AuthTokenService authTokenService;
  private final RefreshTokenCookieWriter refreshTokenCookieWriter;
  private final Oauth2LoginFailureHandler failureHandler;

  @Value("${mopl.oauth2.success-redirect-uri:/}")
  private String successRedirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    try {
      OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
      SocialProvider provider =
          SocialProvider.fromRegistrationId(oauth2Token.getAuthorizedClientRegistrationId());
      Oauth2UserProfile profile =
          profileResolver.resolve(provider, oauth2Token.getPrincipal().getAttributes());
      User user = socialLoginService.findOrLinkUser(profile);

      response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
      response.setHeader(HttpHeaders.PRAGMA, "no-cache");
      response.setDateHeader(HttpHeaders.EXPIRES, 0);

      IssuedToken issuedToken = authTokenService.issue(user.getId());
      refreshTokenCookieWriter.addRefreshTokenCookie(response, issuedToken.refreshToken());
      response.sendRedirect(successRedirectUri);
    } catch (RuntimeException exception) {
      failureHandler.onAuthenticationFailure(
          request, response, new AuthenticationServiceException(exception.getMessage(), exception));
    }
  }
}
