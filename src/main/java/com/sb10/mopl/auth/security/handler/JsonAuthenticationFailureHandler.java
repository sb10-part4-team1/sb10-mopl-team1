package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.exception.InvalidSignInRequestException;
import com.sb10.mopl.common.exception.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final AuthErrorResponseWriter responseWriter;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    if (exception instanceof InvalidSignInRequestException invalidRequestException) {
      responseWriter.write(
          response, SystemErrorCode.INVALID_INPUT_VALUE, invalidRequestException.getDetails());
      return;
    }

    responseWriter.write(
        response,
        AuthErrorCode.AUTHENTICATION_FAILED,
        Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
  }
}
