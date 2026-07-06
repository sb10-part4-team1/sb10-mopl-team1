package com.sb10.mopl.auth.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
      @UniqueConstraint(name = "UK_REFRESH_TOKENS_USER", columnNames = "user_id"),
      @UniqueConstraint(name = "UK_REFRESH_TOKENS_TOKEN", columnNames = "token")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  private RefreshToken(User user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public static RefreshToken create(User user, String tokenHash, Instant expiresAt) {
    return new RefreshToken(user, tokenHash, expiresAt);
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }
}
