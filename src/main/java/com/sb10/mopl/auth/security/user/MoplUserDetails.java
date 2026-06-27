package com.sb10.mopl.auth.security.user;

import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MoplUserDetails implements UserDetails {

  private final UUID id;
  private final Instant createdAt;
  private final String email;
  private final String name;
  private final String encodedPassword;
  private final String profileImageUrl;
  private final UserRole role;
  private final boolean locked;

  public MoplUserDetails(User user) {
    this.id = user.getId();
    this.createdAt = user.getCreatedAt();
    this.email = user.getEmail();
    this.name = user.getName();
    this.encodedPassword = user.getEncodedPassword();
    this.profileImageUrl = user.getProfileImageUrl();
    this.role = user.getRole();
    this.locked = user.isLocked();
  }

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getProfileImageUrl() {
    return profileImageUrl;
  }

  public UserRole getRole() {
    return role;
  }

  public boolean isLocked() {
    return locked;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return encodedPassword;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !locked;
  }
}
