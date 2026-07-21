package com.sb10.mopl.auth.security.jwt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  private static final String VALID_SECRET = "mopl-test-only-jwt-secret-key-at-least-32-bytes";

  @Test
  @DisplayName("모든 값이 유효하면 정상 생성된다")
  void constructor_success_whenAllValuesAreValid() {
    // given & when
    JwtProperties properties =
        new JwtProperties(VALID_SECRET, Duration.ofHours(1), Duration.ofDays(14), validCookie());

    // then
    assertAll(
        () -> assertEquals(VALID_SECRET, properties.secret()),
        () -> assertEquals(Duration.ofHours(1), properties.accessTokenExpiration()),
        () -> assertEquals(Duration.ofDays(14), properties.refreshTokenExpiration()));
  }

  @Test
  @DisplayName("secret이 32바이트 미만이면 예외를 발생시킨다")
  void constructor_throws_whenSecretIsShorterThan32Bytes() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JwtProperties(
                "too-short-secret", Duration.ofHours(1), Duration.ofDays(14), validCookie()));
  }

  @Test
  @DisplayName("accessTokenExpiration이 0이거나 음수이면 예외를 발생시킨다")
  void constructor_throws_whenAccessTokenExpirationIsZeroOrNegative() {
    // when & then
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new JwtProperties(
                        VALID_SECRET, Duration.ZERO, Duration.ofDays(14), validCookie())),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new JwtProperties(
                        VALID_SECRET, Duration.ofHours(-1), Duration.ofDays(14), validCookie())));
  }

  @Test
  @DisplayName("refreshTokenExpiration이 0이거나 음수이면 예외를 발생시킨다")
  void constructor_throws_whenRefreshTokenExpirationIsZeroOrNegative() {
    // when & then
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new JwtProperties(
                        VALID_SECRET, Duration.ofHours(1), Duration.ZERO, validCookie())),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new JwtProperties(
                        VALID_SECRET, Duration.ofHours(1), Duration.ofDays(-1), validCookie())));
  }

  @Test
  @DisplayName("쿠키 경로가 '/'로 시작하지 않으면 예외를 발생시킨다")
  void refreshTokenCookie_throws_whenPathDoesNotStartWithSlash() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JwtProperties.RefreshTokenCookie("REFRESH_TOKEN", "api/auth", true, false, "Lax"));
  }

  @Test
  @DisplayName("httpOnly가 false이면 예외를 발생시킨다")
  void refreshTokenCookie_throws_whenHttpOnlyIsFalse() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JwtProperties.RefreshTokenCookie(
                "REFRESH_TOKEN", "/api/auth", false, false, "Lax"));
  }

  @Test
  @DisplayName("SameSite 값이 Strict/Lax/None이 아니면 예외를 발생시킨다")
  void refreshTokenCookie_throws_whenSameSiteIsUnsupportedValue() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JwtProperties.RefreshTokenCookie(
                "REFRESH_TOKEN", "/api/auth", true, false, "Invalid"));
  }

  @Test
  @DisplayName("SameSite가 None인데 secure가 false이면 예외를 발생시킨다")
  void refreshTokenCookie_throws_whenSameSiteIsNoneAndSecureIsFalse() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JwtProperties.RefreshTokenCookie(
                "REFRESH_TOKEN", "/api/auth", true, false, "None"));
  }

  @Test
  @DisplayName("SameSite가 None이고 secure가 true이면 정상 생성된다")
  void refreshTokenCookie_success_whenSameSiteIsNoneAndSecureIsTrue() {
    // when & then
    assertDoesNotThrow(
        () ->
            new JwtProperties.RefreshTokenCookie("REFRESH_TOKEN", "/api/auth", true, true, "None"));
  }

  private JwtProperties.RefreshTokenCookie validCookie() {
    return new JwtProperties.RefreshTokenCookie("REFRESH_TOKEN", "/api/auth", true, false, "Lax");
  }
}
