package com.sb10.mopl.auth.security.handler;

import com.sb10.mopl.common.exception.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  private final AuthErrorResponseWriter responseWriter;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {

    responseWriter.write(
        response, SystemErrorCode.ACCESS_DENIED, Map.of("message", "요청한 리소스에 접근할 권한이 없습니다."));
  }
}
