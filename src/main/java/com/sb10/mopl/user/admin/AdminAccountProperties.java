package com.sb10.mopl.user.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "mopl.admin")
public record AdminAccountProperties(Initializer initializer, Account account) {

  public boolean enabled() {
    return initializer != null && initializer.enabled();
  }

  public boolean overwritePassword() {
    return initializer != null && initializer.overwritePassword();
  }

  public String email() {
    return account == null ? null : account.email();
  }

  public String name() {
    return account == null ? null : account.name();
  }

  public String password() {
    return account == null ? null : account.password();
  }

  public void validateForInitialization() {
    if (!enabled()) {
      return;
    }

    if (!StringUtils.hasText(email())) {
      throw new IllegalStateException("Admin account email must be configured.");
    }

    if (!StringUtils.hasText(name())) {
      throw new IllegalStateException("Admin account name must be configured.");
    }

    if (!StringUtils.hasText(password())) {
      throw new IllegalStateException("Admin account password must be configured.");
    }
  }

  public record Initializer(boolean enabled, boolean overwritePassword) {}

  public record Account(String email, String name, String password) {}
}
