package com.sb10.mopl.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtSessionTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = createUser(UUID.randomUUID());
  }

  @Test
  @DisplayName("JWT 세션 - 생성 성공")
  void create_success() {
    // given
    UUID sessionId = UUID.randomUUID();
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");

    // when
    JwtSession jwtSession = JwtSession.create(user, sessionId, expiresAt);

    // then
    assertThat(jwtSession.getUser()).isEqualTo(user);
    assertThat(jwtSession.getSessionId()).isEqualTo(sessionId);
    assertThat(jwtSession.getExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("JWT 세션 - 만료 전이면 만료되지 않음")
  void isExpired_false_beforeExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    JwtSession jwtSession = JwtSession.create(user, UUID.randomUUID(), expiresAt);

    // when & then
    assertThat(jwtSession.isExpired(expiresAt.minusSeconds(1))).isFalse();
  }

  @Test
  @DisplayName("JWT 세션 - 정확히 만료 시각이면 만료됨")
  void isExpired_true_atExpirationBoundary() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    JwtSession jwtSession = JwtSession.create(user, UUID.randomUUID(), expiresAt);

    // when & then
    assertThat(jwtSession.isExpired(expiresAt)).isTrue();
  }

  @Test
  @DisplayName("JWT 세션 - 만료 시각 이후면 만료됨")
  void isExpired_true_afterExpiration() {
    // given
    Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
    JwtSession jwtSession = JwtSession.create(user, UUID.randomUUID(), expiresAt);

    // when & then
    assertThat(jwtSession.isExpired(expiresAt.plusSeconds(1))).isTrue();
  }

  @Test
  @DisplayName("JWT 세션 - 만료 시각 연장 성공")
  void extendExpiresAt_success() {
    // given
    Instant originalExpiresAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant extendedExpiresAt = Instant.parse("2026-02-01T00:00:00Z");
    JwtSession jwtSession = JwtSession.create(user, UUID.randomUUID(), originalExpiresAt);

    // when
    jwtSession.extendExpiresAt(extendedExpiresAt);

    // then
    assertThat(jwtSession.getExpiresAt()).isEqualTo(extendedExpiresAt);
  }

  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
