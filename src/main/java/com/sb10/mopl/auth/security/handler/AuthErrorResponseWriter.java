package com.sb10.mopl.auth.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.common.exception.ErrorCode;
import com.sb10.mopl.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthErrorResponseWriter {

  private final ObjectMapper objectMapper;

  public void write(HttpServletResponse response, ErrorCode errorCode, Map<String, Object> details)
      throws IOException {
    response.setStatus(errorCode.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getWriter(),
        new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details));
  }
}
