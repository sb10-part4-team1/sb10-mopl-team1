package com.sb10.mopl.auth.oauth;

import com.sb10.mopl.auth.entity.SocialProvider;

public record Oauth2UserProfile(
    SocialProvider provider,
    String providerUserId,
    String email,
    String name,
    String profileImageUrl) {

  public Oauth2UserProfile {
    if (provider == null) {
      throw new IllegalArgumentException("provider must not be null.");
    }
    validateNotBlank(providerUserId, "providerUserId");
    validateNotBlank(email, "email");
    validateNotBlank(name, "name");
  }

  private static void validateNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank.");
    }
  }
}
