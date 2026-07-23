package com.sb10.mopl.auth.security.principal;

import com.sb10.mopl.user.entity.UserRole;
import java.io.Serializable;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) implements Serializable {
  private static final long serialVersionUID = 1L;

  public String roleName() {
    return role.name();
  }

  public String authorityName() {
    return role.authorityName();
  }
}
