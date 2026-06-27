package com.sb10.mopl.auth.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.dto.response.JwtDto;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.user.dto.response.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    MoplUserDetails userDetails = (MoplUserDetails) authentication.getPrincipal();
    UserDto userDto =
        new UserDto(
            userDetails.getId(),
            userDetails.getCreatedAt(),
            userDetails.getEmail(),
            userDetails.getName(),
            userDetails.getProfileImageUrl(),
            userDetails.getRole(),
            userDetails.isLocked());

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    response.setDateHeader(HttpHeaders.EXPIRES, 0);

    JwtDto jwtDto = new JwtDto(userDto, jwtProvider.createAccessToken(userDetails));
    objectMapper.writeValue(response.getWriter(), jwtDto);
  }
}
