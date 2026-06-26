package com.sb10.mopl.auth.security.user;

import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MoplUserDetails implements UserDetails {

  private final User user;

  public MoplUserDetails(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public UUID getId() {
    return user.getId();
  }

  public String getEmail() {
    return user.getEmail();
  }

  public UserRole getRole() {
    return user.getRole();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
  }

  @Override
  public String getPassword() {
    return user.getEncodedPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  @Override
  public boolean isAccountNonLocked() {
    return !user.isLocked();
  }
}
