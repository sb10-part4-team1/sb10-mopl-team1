package com.sb10.mopl.auth.security.user;

import com.sb10.mopl.user.entity.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {

  public String roleName() {
    return role.name();
  }

  public String authorityName() {
    return role.authorityName();
  }
}
