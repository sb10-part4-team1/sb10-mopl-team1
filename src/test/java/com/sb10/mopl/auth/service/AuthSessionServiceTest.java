package com.sb10.mopl.auth.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

  @Mock private JwtSessionService jwtSessionService;

  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthSessionService authSessionService;

  @Test
  @DisplayName("사용자 인증 세션 무효화 시 JwtSession과 RefreshToken을 함께 정리한다")
  void invalidateAllByUserId_invalidatesJwtSessionAndRefreshToken() {
    UUID userId = UUID.randomUUID();

    authSessionService.invalidateAllByUserId(userId);

    verify(jwtSessionService).invalidateByUserId(userId);
    verify(refreshTokenService).revokeAllByUserId(userId);
    verifyNoMoreInteractions(jwtSessionService, refreshTokenService);
  }
}
