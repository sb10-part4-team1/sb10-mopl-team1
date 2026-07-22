package com.sb10.mopl.auth.controller;

import com.sb10.mopl.auth.controller.api.AuthControllerApiDocs;
import com.sb10.mopl.auth.dto.request.ResetPasswordRequest;
import com.sb10.mopl.auth.dto.response.JwtDto;
import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.auth.service.AuthTokenService;
import com.sb10.mopl.auth.service.AuthTokenService.ReissuedToken;
import com.sb10.mopl.auth.service.TemporaryPasswordService;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerApiDocs {

  private final AuthTokenService authTokenService;
  private final TemporaryPasswordService temporaryPasswordService;
  private final RefreshTokenCookieWriter refreshTokenCookieWriter;
  private final JwtProvider jwtProvider;

  @Override
  @PostMapping("/refresh")
  public JwtDto reissueToken(
      @CookieValue(name = "${mopl.jwt.refresh-token-cookie.name}", required = false)
          String refreshToken,
      HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    response.setDateHeader(HttpHeaders.EXPIRES, 0);

    ReissuedToken reissuedToken = authTokenService.reissue(refreshToken);

    refreshTokenCookieWriter.addRefreshTokenCookie(response, reissuedToken.refreshToken());

    User user = reissuedToken.user();
    UserDto userDto =
        new UserDto(
            user.getId(),
            user.getCreatedAt(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getRole(),
            user.isLocked());
    return new JwtDto(
        userDto,
        jwtProvider.createAccessToken(new MoplUserDetails(user), reissuedToken.sessionId()));
  }

  @Override
  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
    temporaryPasswordService.resetPassword(resetPasswordRequest.email());
    return ResponseEntity.noContent().build();
  }
}
