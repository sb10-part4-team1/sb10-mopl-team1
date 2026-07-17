package com.sb10.mopl.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TemporaryPasswordTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = createUser(UUID.randomUUID());
  }

  @Test
  @DisplayName("임시 비밀번호 - 생성 성공")
  void create_success() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");

    // when
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // then
    assertThat(tempPassword.getUser()).isEqualTo(user);
    assertThat(tempPassword.getPasswordHash()).isEqualTo("hashed-password");
    assertThat(tempPassword.getExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("임시 비밀번호 - 만료 전이면 만료되지 않음")
  void isExpired_false_beforeExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // when & then
    assertThat(tempPassword.isExpired(expiresAt.minusSeconds(1))).isFalse();
  }

  @Test
  @DisplayName("임시 비밀번호 - 정확히 만료 시각이면 만료됨")
  void isExpired_true_atExpirationBoundary() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // when & then
    assertThat(tempPassword.isExpired(expiresAt)).isTrue();
  }

  @Test
  @DisplayName("임시 비밀번호 - 만료 시각 이후면 만료됨")
  void isExpired_true_afterExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // when & then
    assertThat(tempPassword.isExpired(expiresAt.plusSeconds(1))).isTrue();
  }

  @Test
  @DisplayName("임시 비밀번호 - 만료 전이면 유효함")
  void isValid_true_beforeExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // when & then
    assertThat(tempPassword.isValid(expiresAt.minusSeconds(1))).isTrue();
  }

  @Test
  @DisplayName("임시 비밀번호 - 정확히 만료 시각이면 유효하지 않음")
  void isValid_false_atExpirationBoundary() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // when & then
    assertThat(tempPassword.isValid(expiresAt)).isFalse();
  }

  @Test
  @DisplayName("임시 비밀번호 - 만료 시각 이후면 유효하지 않음")
  void isValid_false_afterExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    TemporaryPassword tempPassword = TemporaryPassword.create(user, "hashed-password", expiresAt);

    // when & then
    assertThat(tempPassword.isValid(expiresAt.plusSeconds(1))).isFalse();
  }

  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
