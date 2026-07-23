package com.sb10.mopl.auth.security.jwt;

import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * JWT claims를 AuthenticatedUser로 변환한다. JwtAuthenticationFilter와 StompChannelInterceptor가 각자 구현을 두면
 * JWT 정책 변경 시 한쪽만 수정되어 인증 결과가 어긋날 수 있어 공용으로 분리했다.
 */
@Component
public class AuthenticatedUserFactory {

  public AuthenticatedUser from(Claims claims) {
    String subject = claims.getSubject();
    String id = requiredClaim(claims, "id");
    String email = requiredClaim(claims, "email");
    String role = requiredClaim(claims, "role");
    String tokenType = requiredClaim(claims, JwtProvider.TOKEN_TYPE_CLAIM);

    if (subject == null || subject.isBlank() || !subject.equals(id)) {
      throw new IllegalArgumentException("JWT subject does not match id claim.");
    }

    if (!JwtProvider.ACCESS_TOKEN_TYPE.equals(tokenType)) {
      throw new IllegalArgumentException("JWT token type is not ACCESS.");
    }

    return new AuthenticatedUser(UUID.fromString(id), email, UserRole.valueOf(role));
  }

  public String requiredClaim(Claims claims, String name) {
    Object value = claims.get(name);
    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      throw new IllegalArgumentException("Missing JWT claim: " + name);
    }
    return stringValue;
  }
}
