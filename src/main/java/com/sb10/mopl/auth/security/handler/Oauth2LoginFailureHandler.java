package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.common.exception.MoplException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class Oauth2LoginFailureHandler implements AuthenticationFailureHandler {

  private static final String ACCESS_DENIED_ERROR_CODE = "access_denied";
  private static final String ACCESS_DENIED_ERROR_MESSAGE = "소셜 로그인 요청이 취소되었거나 권한이 거부되었습니다.";
  private static final String DEFAULT_ERROR_MESSAGE = "소셜 로그인에 실패했습니다.";

  @Value("${mopl.oauth2.failure-redirect-uri:/#/sign-in}")
  private String failureRedirectUri;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    response.sendRedirect(
        appendQuery(
            failureRedirectUri,
            "error=oauth_failed&error_message="
                + URLEncoder.encode(resolveErrorMessage(exception), StandardCharsets.UTF_8)));
  }

  private String resolveErrorMessage(AuthenticationException exception) {
    if (exception instanceof OAuth2AuthenticationException oauth2Exception
        && ACCESS_DENIED_ERROR_CODE.equals(oauth2Exception.getError().getErrorCode())) {
      return ACCESS_DENIED_ERROR_MESSAGE;
    }
    if (exception.getCause() instanceof MoplException moplException) {
      Object message = moplException.getDetails().get("message");
      if (message instanceof String detailMessage && !detailMessage.isBlank()) {
        return detailMessage;
      }
    }
    return DEFAULT_ERROR_MESSAGE;
  }

  private String appendQuery(String uri, String query) {
    return uri + (uri.contains("?") ? "&" : "?") + query;
  }
}
