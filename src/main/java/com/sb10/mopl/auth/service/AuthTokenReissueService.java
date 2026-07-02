package com.sb10.mopl.auth.service;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.service.RefreshTokenService.IssuedRefreshToken;
import com.sb10.mopl.auth.service.RefreshTokenService.RotatedRefreshToken;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.user.entity.User;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTokenReissueService {

  private final RefreshTokenService refreshTokenService;
  private final JwtSessionService jwtSessionService;

  @Transactional
  public ReissuedToken reissue(String refreshToken) {
    RotatedRefreshToken rotatedRefreshToken =
        refreshTokenService
            .rotate(refreshToken)
            .orElseThrow(
                () ->
                    new MoplException(
                        AuthErrorCode.AUTHENTICATION_FAILED,
                        Map.of("message", "유효하지 않은 리프레시 토큰입니다.")));

    User user = rotatedRefreshToken.user();
    IssuedRefreshToken issuedRefreshToken = rotatedRefreshToken.refreshToken();
    UUID sessionId =
        jwtSessionService
            .extendActiveSession(user.getId(), issuedRefreshToken.expiresAt())
            .orElseThrow(
                () ->
                    new MoplException(
                        AuthErrorCode.AUTHENTICATION_FAILED,
                        Map.of("message", "유효하지 않은 로그인 세션입니다.")));

    return new ReissuedToken(issuedRefreshToken, user, sessionId);
  }

  public record ReissuedToken(IssuedRefreshToken refreshToken, User user, UUID sessionId) {}
}
