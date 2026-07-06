package com.sb10.mopl.auth.security.jwt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mopl.jwt")
public record JwtProperties(
    @NotBlank String secret,
    @NotNull Duration accessTokenExpiration,
    @NotNull Duration refreshTokenExpiration,
    @Valid @NotNull RefreshTokenCookie refreshTokenCookie) {

  private static final int MIN_HMAC_SECRET_BYTES = 32;
  private static final Set<String> SUPPORTED_SAME_SITE_VALUES = Set.of("Strict", "Lax", "None");

  public JwtProperties {
    if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MIN_HMAC_SECRET_BYTES) {
      throw new IllegalArgumentException("JWT secret must be at least 32 bytes.");
    }

    if (accessTokenExpiration != null
        && (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative())) {
      throw new IllegalArgumentException("JWT access token expiration must be positive.");
    }

    if (refreshTokenExpiration != null
        && (refreshTokenExpiration.isZero() || refreshTokenExpiration.isNegative())) {
      throw new IllegalArgumentException("JWT refresh token expiration must be positive.");
    }
  }

  public record RefreshTokenCookie(
      @NotBlank String name,
      @NotBlank String path,
      boolean httpOnly,
      boolean secure,
      @NotBlank String sameSite) {

    public RefreshTokenCookie {
      if (path != null && !path.startsWith("/")) {
        throw new IllegalArgumentException("Refresh token cookie path must start with '/'.");
      }

      if (!httpOnly) {
        throw new IllegalArgumentException("Refresh token cookie must be HttpOnly.");
      }

      if (sameSite != null && !SUPPORTED_SAME_SITE_VALUES.contains(sameSite)) {
        throw new IllegalArgumentException(
            "Refresh token cookie SameSite must be Strict, Lax, or None.");
      }

      if ("None".equals(sameSite) && !secure) {
        throw new IllegalArgumentException(
            "Refresh token cookie with SameSite=None must be Secure.");
      }
    }
  }
}
