package com.sb10.mopl.auth.controller;

import com.sb10.mopl.auth.dto.response.JwtDto;
import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.auth.service.RefreshTokenService;
import com.sb10.mopl.auth.service.RefreshTokenService.RotatedRefreshToken;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookieWriter refreshTokenCookieWriter;
  private final JwtProvider jwtProvider;

  @PostMapping("/refresh")
  public JwtDto reissueToken(
      @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    RotatedRefreshToken rotatedRefreshToken =
        refreshTokenService
            .rotate(refreshToken)
            .orElseThrow(
                () ->
                    new MoplException(
                        AuthErrorCode.AUTHENTICATION_FAILED,
                        Map.of("message", "유효하지 않은 리프레시 토큰입니다.")));

    refreshTokenCookieWriter.addRefreshTokenCookie(response, rotatedRefreshToken.refreshToken());

    User user = rotatedRefreshToken.user();
    UserDto userDto =
        new UserDto(
            user.getId(),
            user.getCreatedAt(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getRole(),
            user.isLocked());
    return new JwtDto(userDto, jwtProvider.createAccessToken(new MoplUserDetails(user)));
  }
}
