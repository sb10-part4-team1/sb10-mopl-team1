package com.sb10.mopl.auth.security.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JsonAuthenticationEntryPointTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("인증 진입점은 인증 실패 응답을 JSON으로 작성한다")
  void commence_writesAuthenticationFailureResponse() throws Exception {
    JsonAuthenticationEntryPoint entryPoint =
        new JsonAuthenticationEntryPoint(new AuthErrorResponseWriter(objectMapper));
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(
        new MockHttpServletRequest(),
        response,
        new BadCredentialsException("authentication required"));

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertEquals(401, response.getStatus());
    assertEquals("AUTH01", body.get("code").asText());
    assertEquals("Authentication is required.", body.get("details").get("message").asText());
  }
}
