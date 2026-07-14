package com.sb10.mopl.auth.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "jwt_sessions",
    uniqueConstraints = {
      @UniqueConstraint(name = "UK_JWT_SESSIONS_USER", columnNames = "user_id"),
      @UniqueConstraint(name = "UK_JWT_SESSIONS_SESSION", columnNames = "session_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JwtSession extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "session_id", nullable = false, unique = true, updatable = false)
  private UUID sessionId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  private JwtSession(User user, UUID sessionId, Instant expiresAt) {
    this.user = user;
    this.sessionId = sessionId;
    this.expiresAt = expiresAt;
  }

  public static JwtSession create(User user, UUID sessionId, Instant expiresAt) {
    return new JwtSession(user, sessionId, expiresAt);
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public void extendExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }
}
