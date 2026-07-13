package com.sb10.mopl.auth.entity;

import java.util.Locale;

public enum SocialProvider {
  GOOGLE,
  KAKAO;

  public static SocialProvider fromRegistrationId(String registrationId) {
    if (registrationId == null || registrationId.isBlank()) {
      throw new IllegalArgumentException("OAuth2 registrationId must not be blank.");
    }
    return SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
  }

  public String registrationId() {
    return name().toLowerCase(Locale.ROOT);
  }
}
