package com.sb10.mopl.auth.oauth;

import com.sb10.mopl.auth.entity.SocialProvider;
import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoogleOauth2UserProfileResolver implements Oauth2UserProfileResolver {

  @Override
  public SocialProvider provider() {
    return SocialProvider.GOOGLE;
  }

  @Override
  public Oauth2UserProfile resolve(Map<String, Object> attributes) {
    String providerUserId = stringValue(attributes.get("sub"));
    String email = stringValue(attributes.get("email"));
    String name = stringValue(attributes.get("name"));
    String profileImageUrl = stringValue(attributes.get("picture"));

    if (!Boolean.TRUE.equals(attributes.get("email_verified"))) {
      throw new MoplException(
          AuthErrorCode.AUTHENTICATION_FAILED,
          Map.of("message", "Google account email is not verified."));
    }

    return new Oauth2UserProfile(
        SocialProvider.GOOGLE, providerUserId, email, resolveName(name, email), profileImageUrl);
  }

  private String resolveName(String name, String email) {
    if (name != null && !name.isBlank()) {
      return name;
    }
    return email;
  }

  private String stringValue(Object value) {
    return value instanceof String string ? string : null;
  }
}
