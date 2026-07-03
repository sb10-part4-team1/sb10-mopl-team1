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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "temporary_passwords",
    uniqueConstraints = {
      @UniqueConstraint(name = "UK_TEMPORARY_PASSWORDS_USER", columnNames = "user_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemporaryPassword extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "password", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  private TemporaryPassword(User user, String passwordHash, Instant expiresAt) {
    this.user = user;
    this.passwordHash = passwordHash;
    this.expiresAt = expiresAt;
  }

  public static TemporaryPassword create(User user, String passwordHash, Instant expiresAt) {
    return new TemporaryPassword(user, passwordHash, expiresAt);
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public boolean isValid(Instant now) {
    return expiresAt.isAfter(now);
  }
}
