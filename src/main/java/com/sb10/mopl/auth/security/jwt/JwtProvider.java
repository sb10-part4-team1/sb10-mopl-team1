package com.sb10.mopl.auth.security.jwt;

import com.sb10.mopl.auth.security.user.MoplUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  public static final String TOKEN_TYPE_CLAIM = "tokenType";
  public static final String ACCESS_TOKEN_TYPE = "ACCESS";

  private final JwtProperties jwtProperties;
  private final Clock clock;
  private final SecretKey secretKey;

  public JwtProvider(JwtProperties jwtProperties, Clock clock) {
    this.jwtProperties = jwtProperties;
    this.clock = clock;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(MoplUserDetails userDetails) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenExpiration());

    return Jwts.builder()
        .subject(userDetails.getId().toString())
        .claim("id", userDetails.getId().toString())
        .claim("email", userDetails.getEmail())
        .claim("role", userDetails.getRole().name())
        .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .clock(() -> Date.from(clock.instant()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
