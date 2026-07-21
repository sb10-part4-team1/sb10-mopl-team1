package com.sb10.mopl.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = createUser(UUID.randomUUID());
  }

  @Test
  @DisplayName("리프레시 토큰 - 생성 성공")
  void create_success() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");

    // when
    RefreshToken refreshToken = RefreshToken.create(user, "hashed-token", expiresAt);

    // then
    assertThat(refreshToken.getUser()).isEqualTo(user);
    assertThat(refreshToken.getTokenHash()).isEqualTo("hashed-token");
    assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("리프레시 토큰 - 만료 전이면 만료되지 않음")
  void isExpired_false_beforeExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    RefreshToken refreshToken = RefreshToken.create(user, "hashed-token", expiresAt);

    // when & then
    assertThat(refreshToken.isExpired(expiresAt.minusSeconds(1))).isFalse();
  }

  @Test
  @DisplayName("리프레시 토큰 - 정확히 만료 시각이면 만료됨")
  void isExpired_true_atExpirationBoundary() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    RefreshToken refreshToken = RefreshToken.create(user, "hashed-token", expiresAt);

    // when & then
    assertThat(refreshToken.isExpired(expiresAt)).isTrue();
  }

  @Test
  @DisplayName("리프레시 토큰 - 만료 시각 이후면 만료됨")
  void isExpired_true_afterExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    RefreshToken refreshToken = RefreshToken.create(user, "hashed-token", expiresAt);

    // when & then
    assertThat(refreshToken.isExpired(expiresAt.plusSeconds(1))).isTrue();
  }

  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
