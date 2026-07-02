package com.sb10.mopl.auth.service;

import com.sb10.mopl.auth.entity.JwtSession;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JwtSessionService {

  private final JwtSessionRepository jwtSessionRepository;
  private final UserRepository userRepository;
  private final JwtProperties jwtProperties;
  private final Clock clock;

  @Transactional
  public IssuedJwtSession issue(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    invalidateByUserId(userId);

    UUID sessionId = UUID.randomUUID();
    Instant expiresAt = clock.instant().plus(jwtProperties.refreshTokenExpiration());
    jwtSessionRepository.save(JwtSession.create(user, sessionId, expiresAt));
    return new IssuedJwtSession(sessionId, expiresAt);
  }

  @Transactional(readOnly = true)
  public Optional<UUID> findActiveSessionId(UUID userId) {
    return jwtSessionRepository
        .findActiveByUserId(userId, clock.instant())
        .map(JwtSession::getSessionId);
  }

  @Transactional
  public Optional<UUID> extendActiveSession(UUID userId, Instant expiresAt) {
    return jwtSessionRepository
        .findActiveByUserId(userId, clock.instant())
        .map(
            jwtSession -> {
              jwtSession.extendExpiresAt(expiresAt);
              return jwtSession.getSessionId();
            });
  }

  @Transactional(readOnly = true)
  public boolean isActive(UUID userId, UUID sessionId) {
    if (userId == null || sessionId == null) {
      return false;
    }
    return jwtSessionRepository.existsActiveSession(userId, sessionId, clock.instant());
  }

  @Transactional
  public void invalidateByUserId(UUID userId) {
    jwtSessionRepository.deleteByUserId(userId);
  }

  @Transactional
  public int deleteExpiredSessions() {
    return jwtSessionRepository.deleteByExpiresAtLessThanEqual(clock.instant());
  }

  public record IssuedJwtSession(UUID sessionId, Instant expiresAt) {}
}
