package com.sb10.mopl.auth.security.jwt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtProviderTest {

  private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
  private static final String SECRET = "mopl-test-only-jwt-secret-key-at-least-32-bytes";

  @Test
  @DisplayName("발급한 액세스 토큰을 파싱하면 발급 시 사용한 클레임을 그대로 되돌려준다")
  void createAccessToken_thenParseClaims_returnsSameClaims() {
    // given
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    MoplUserDetails userDetails = userDetails(userId, "jwt-user@example.com", UserRole.USER);
    JwtProvider jwtProvider = jwtProviderAt(NOW);

    // when
    String token = jwtProvider.createAccessToken(userDetails, sessionId);
    Claims claims = jwtProvider.parseClaims(token);

    // then
    assertAll(
        () -> assertEquals(userId.toString(), claims.getSubject()),
        () -> assertEquals(userId.toString(), claims.get("id", String.class)),
        () -> assertEquals("jwt-user@example.com", claims.get("email", String.class)),
        () -> assertEquals(UserRole.USER.name(), claims.get("role", String.class)),
        () ->
            assertEquals(
                JwtProvider.ACCESS_TOKEN_TYPE,
                claims.get(JwtProvider.TOKEN_TYPE_CLAIM, String.class)),
        () ->
            assertEquals(
                sessionId.toString(), claims.get(JwtProvider.SESSION_ID_CLAIM, String.class)));
  }

  @Test
  @DisplayName("만료된 토큰을 파싱하면 ExpiredJwtException을 발생시킨다")
  void parseClaims_throwsExpiredJwtException_whenTokenIsExpired() {
    // given
    MoplUserDetails userDetails =
        userDetails(UUID.randomUUID(), "expired@example.com", UserRole.USER);
    String token = jwtProviderAt(NOW).createAccessToken(userDetails, UUID.randomUUID());
    JwtProvider expiredContextProvider = jwtProviderAt(NOW.plus(Duration.ofHours(2)));

    // when & then
    assertThrows(ExpiredJwtException.class, () -> expiredContextProvider.parseClaims(token));
  }

  private JwtProvider jwtProviderAt(Instant instant) {
    JwtProperties properties =
        new JwtProperties(
            SECRET,
            Duration.ofHours(1),
            Duration.ofDays(14),
            new JwtProperties.RefreshTokenCookie("REFRESH_TOKEN", "/api/auth", true, false, "Lax"));
    return new JwtProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
  }

  private MoplUserDetails userDetails(UUID id, String email, UserRole role) {
    User user = User.createUser("jwt-user", email, "encoded-password", null);
    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "createdAt", NOW);
    return new MoplUserDetails(user);
  }
}
