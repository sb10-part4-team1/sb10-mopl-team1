package com.sb10.mopl.auth.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class Oauth2LoginFailureHandler implements AuthenticationFailureHandler {

  private static final String DEFAULT_ERROR_MESSAGE = "소셜 로그인에 실패했습니다.";

  @Value("${mopl.oauth2.failure-redirect-uri:/#/sign-in}")
  private String failureRedirectUri;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    String errorMessage =
        exception.getMessage() == null || exception.getMessage().isBlank()
            ? DEFAULT_ERROR_MESSAGE
            : exception.getMessage();
    response.sendRedirect(
        appendQuery(
            failureRedirectUri,
            "error=oauth_failed&error_message="
                + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)));
  }

  private String appendQuery(String uri, String query) {
    return uri + (uri.contains("?") ? "&" : "?") + query;
  }
}
