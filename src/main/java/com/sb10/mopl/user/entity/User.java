package com.sb10.mopl.user.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {@UniqueConstraint(name = "UK_USERS_EMAIL", columnNames = "email")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Getter(AccessLevel.NONE)
  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @Column(name = "profile_image_url")
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private UserRole role;

  @Column(name = "is_locked", nullable = false)
  private boolean isLocked;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  private User(String name, String email, String password, String profileImageUrl, UserRole role) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.profileImageUrl = profileImageUrl;
    this.role = role;
    this.isLocked = false;
    this.isDeleted = false;
  }

  public static User createUser(
      String name, String email, String password, String profileImageUrl) {
    return new User(name, email, password, profileImageUrl, UserRole.USER);
  }

  public static User createAdmin(
      String name, String email, String password, String profileImageUrl) {
    return new User(name, email, password, profileImageUrl, UserRole.ADMIN);
  }

  public String getEncodedPassword() {
    return password;
  }

  public void changePassword(String password) {
    this.password = password;
  }

  public void changeRole(UserRole role) {
    this.role = role;
  }

  public void changeLocked(boolean locked) {
    this.isLocked = locked;
  }

  public void softDelete() {
    if (this.deletedAt == null) {
      this.deletedAt = Instant.now();
    }
    isDeleted = true;
  }
}
