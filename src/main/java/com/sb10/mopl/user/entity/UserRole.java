package com.sb10.mopl.user.entity;

public enum UserRole {
  USER,
  ADMIN;

  private static final String SPRING_SECURITY_ROLE_PREFIX = "ROLE_";

  public String authorityName() {
    return SPRING_SECURITY_ROLE_PREFIX + name();
  }
}
