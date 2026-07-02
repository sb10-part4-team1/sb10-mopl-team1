package com.sb10.mopl.auth.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

  private final JwtSessionService jwtSessionService;
  private final RefreshTokenService refreshTokenService;

  @Transactional
  public void invalidateAllByUserId(UUID userId) {
    jwtSessionService.invalidateByUserId(userId);
    refreshTokenService.revokeAllByUserId(userId);
  }
}
