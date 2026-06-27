package com.sb10.mopl.auth.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mopl.jwt")
public record JwtProperties(@NotBlank String secret, @NotNull Duration accessTokenExpiration) {

  private static final int MIN_HMAC_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MIN_HMAC_SECRET_BYTES) {
      throw new IllegalArgumentException("JWT secret must be at least 32 bytes.");
    }

    if (accessTokenExpiration != null
        && (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative())) {
      throw new IllegalArgumentException("JWT access token expiration must be positive.");
    }
  }
}
