package com.sb10.mopl.user.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "profile_image_url", nullable = false)
  private String profile;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  @Column(name = "is_locked", nullable = false)
  private boolean isLocked;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public User(String name, String email, String password, String profile, UserRole role) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.profile = profile;
    this.role = role;
    this.isLocked = false;
    this.isDeleted = false;
  }

  public void delete() {
    if (this.deletedAt == null) {
      this.deletedAt = Instant.now();
    }
  }
}
