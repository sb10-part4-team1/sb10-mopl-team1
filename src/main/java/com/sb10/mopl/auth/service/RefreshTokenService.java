package com.sb10.mopl.auth.service;

import com.sb10.mopl.auth.entity.RefreshToken;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final int TOKEN_BYTE_LENGTH = 64;
  private static final String HASH_ALGORITHM = "SHA-256";

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final JwtProperties jwtProperties;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public IssuedRefreshToken issue(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));
    revokeAllByUserId(userId);
    return saveNewTokenFor(user);
  }

  @Transactional
  public Optional<IssuedRefreshToken> rotate(String rawToken) {
    return resolveUsableToken(rawToken)
        .map(
            refreshToken -> {
              User user = refreshToken.getUser();
              refreshTokenRepository.delete(refreshToken);
              refreshTokenRepository.flush();
              return saveNewTokenFor(user);
            });
  }

  @Transactional
  public void revoke(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }

    refreshTokenRepository.deleteByTokenHash(hash(rawToken));
  }

  @Transactional
  public void revokeAllByUserId(UUID userId) {
    refreshTokenRepository.deleteByUserId(userId);
    refreshTokenRepository.flush();
  }

  @Transactional
  public int deleteExpiredTokens() {
    return refreshTokenRepository.deleteByExpiresAtLessThanEqual(clock.instant());
  }

  private Optional<RefreshToken> resolveUsableToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return Optional.empty();
    }

    Instant now = clock.instant();
    return refreshTokenRepository
        .findByTokenHash(hash(rawToken))
        .filter(token -> !token.isExpired(now));
  }

  private IssuedRefreshToken saveNewTokenFor(User user) {
    byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);

    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expiresAt = clock.instant().plus(jwtProperties.refreshTokenExpiration());
    refreshTokenRepository.save(RefreshToken.create(user, hash(rawToken), expiresAt));
    return new IssuedRefreshToken(rawToken, expiresAt);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
    }
  }

  public record IssuedRefreshToken(String rawToken, Instant expiresAt) {}
}
