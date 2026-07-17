package com.sb10.mopl.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.entity.RefreshToken;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-07-17T00:00:00Z");
  private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private UserRepository userRepository;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties =
        new JwtProperties(
            "mopl-test-only-jwt-secret-key-at-least-32-bytes",
            Duration.ofHours(1),
            REFRESH_TOKEN_EXPIRATION,
            new JwtProperties.RefreshTokenCookie("REFRESH_TOKEN", "/api/auth", true, false, "Lax"));
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    refreshTokenService =
        new RefreshTokenService(refreshTokenRepository, userRepository, jwtProperties, fixedClock);
  }

  @Test
  @DisplayName("사용자가 존재하면 기존 토큰을 모두 폐기하고 새 토큰을 발급한다")
  void issue_success_whenUserExists() {
    // given
    UUID userId = UUID.randomUUID();
    User user = user(userId, false);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // when
    RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(userId);

    // then
    assertAll(
        () -> assertFalse(issued.rawToken().isBlank()),
        () -> assertEquals(FIXED_NOW.plus(REFRESH_TOKEN_EXPIRATION), issued.expiresAt()));
    verify(refreshTokenRepository).deleteByUserId(userId);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("사용자가 존재하지 않으면 사용자 없음 예외를 발생시킨다")
  void issue_throwsUserNotFoundException_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> refreshTokenService.issue(userId));

    // then
    assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("전달된 토큰이 null이거나 blank이면 회전 없이 빈 값을 반환한다")
  void rotate_returnsEmpty_whenRawTokenIsNullOrBlank() {
    // when & then
    assertAll(
        () -> assertTrue(refreshTokenService.rotate(null).isEmpty()),
        () -> assertTrue(refreshTokenService.rotate(" ").isEmpty()));
    verify(refreshTokenRepository, never()).findByTokenHashWithUser(anyString());
  }

  @Test
  @DisplayName("토큰이 조회되지 않으면 빈 값을 반환한다")
  void rotate_returnsEmpty_whenTokenNotFound() {
    // given
    when(refreshTokenRepository.findByTokenHashWithUser(anyString())).thenReturn(Optional.empty());

    // when
    Optional<RefreshTokenService.RotatedRefreshToken> result =
        refreshTokenService.rotate("raw-token");

    // then
    assertTrue(result.isEmpty());
    verify(refreshTokenRepository, never()).delete(any());
  }

  @Test
  @DisplayName("토큰이 만료되었으면 빈 값을 반환하고 삭제하지 않는다")
  void rotate_returnsEmpty_whenTokenIsExpired() {
    // given
    User user = user(UUID.randomUUID(), false);
    RefreshToken expiredToken = RefreshToken.create(user, "hash", FIXED_NOW.minusSeconds(1));
    when(refreshTokenRepository.findByTokenHashWithUser(anyString()))
        .thenReturn(Optional.of(expiredToken));

    // when
    Optional<RefreshTokenService.RotatedRefreshToken> result =
        refreshTokenService.rotate("raw-token");

    // then
    assertTrue(result.isEmpty());
    verify(refreshTokenRepository, never()).delete(any());
  }

  @Test
  @DisplayName("사용자가 잠겨 있으면 기존 토큰은 삭제하되 빈 값을 반환한다")
  void rotate_returnsEmpty_whenUserIsLocked() {
    // given
    User user = user(UUID.randomUUID(), true);
    RefreshToken validToken = RefreshToken.create(user, "hash", FIXED_NOW.plusSeconds(60));
    when(refreshTokenRepository.findByTokenHashWithUser(anyString()))
        .thenReturn(Optional.of(validToken));

    // when
    Optional<RefreshTokenService.RotatedRefreshToken> result =
        refreshTokenService.rotate("raw-token");

    // then
    assertTrue(result.isEmpty());
    verify(refreshTokenRepository).delete(validToken);
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("토큰이 유효하고 사용자가 잠겨 있지 않으면 새 토큰을 발급한다")
  void rotate_success_whenTokenValidAndUserNotLocked() {
    // given
    User user = user(UUID.randomUUID(), false);
    RefreshToken validToken = RefreshToken.create(user, "hash", FIXED_NOW.plusSeconds(60));
    when(refreshTokenRepository.findByTokenHashWithUser(anyString()))
        .thenReturn(Optional.of(validToken));

    // when
    Optional<RefreshTokenService.RotatedRefreshToken> result =
        refreshTokenService.rotate("raw-token");

    // then
    assertTrue(result.isPresent());
    assertEquals(user, result.get().user());
    verify(refreshTokenRepository).delete(validToken);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("토큰이 null이거나 blank이면 revoke는 아무 동작도 하지 않는다")
  void revoke_noop_whenRawTokenIsNullOrBlank() {
    // when
    refreshTokenService.revoke(null);
    refreshTokenService.revoke(" ");

    // then
    verify(refreshTokenRepository, never()).deleteByTokenHash(anyString());
  }

  @Test
  @DisplayName("토큰이 존재하면 해시로 삭제한다")
  void revoke_success_deletesByHashedToken() {
    // when
    refreshTokenService.revoke("raw-token");

    // then
    verify(refreshTokenRepository).deleteByTokenHash(anyString());
  }

  @Test
  @DisplayName("사용자 ID로 토큰을 모두 삭제하고 flush 한다")
  void revokeAllByUserId_deletesByUserIdAndFlushes() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    refreshTokenService.revokeAllByUserId(userId);

    // then
    verify(refreshTokenRepository).deleteByUserId(userId);
    verify(refreshTokenRepository).flush();
  }

  @Test
  @DisplayName("현재 시각 기준으로 만료된 토큰을 리포지토리에 위임해 삭제한다")
  void deleteExpiredTokens_delegatesToRepositoryWithClockInstant() {
    // given
    when(refreshTokenRepository.deleteByExpiresAtLessThanEqual(FIXED_NOW)).thenReturn(3);

    // when
    int deletedCount = refreshTokenService.deleteExpiredTokens();

    // then
    assertEquals(3, deletedCount);
    verify(refreshTokenRepository).deleteByExpiresAtLessThanEqual(FIXED_NOW);
  }

  private User user(UUID userId, boolean locked) {
    User user = User.createUser("refresh-token-user", "user@example.com", "encoded-password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    ReflectionTestUtils.setField(user, "isLocked", locked);
    return user;
  }
}
