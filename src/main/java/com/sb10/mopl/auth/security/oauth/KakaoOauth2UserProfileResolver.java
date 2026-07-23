package com.sb10.mopl.auth.security.oauth;

import com.sb10.mopl.auth.entity.SocialProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KakaoOauth2UserProfileResolver implements Oauth2UserProfileResolver {

  private static final String KAKAO_EMAIL_DOMAIN = "kakao.com";
  private static final String DEFAULT_NICKNAME = "kakao";

  @Override
  public SocialProvider provider() {
    return SocialProvider.KAKAO;
  }

  @Override
  public Oauth2UserProfile resolve(Map<String, Object> attributes) {
    String providerUserId = stringValue(attributes.get("id"));

    Map<String, Object> kakaoAccount = castMap(attributes.get("kakao_account"));
    Map<String, Object> accountProfile = castMap(kakaoAccount.get("profile"));
    Map<String, Object> properties = castMap(attributes.get("properties"));

    String nickname =
        firstNonBlank(
            stringValue(accountProfile.get("nickname")),
            stringValue(properties.get("nickname")),
            DEFAULT_NICKNAME);
    String profileImageUrl =
        firstNonBlank(
            stringValue(accountProfile.get("profile_image_url")),
            stringValue(properties.get("profile_image")));

    return new Oauth2UserProfile(
        SocialProvider.KAKAO,
        providerUserId,
        createVirtualEmail(nickname, providerUserId),
        resolveName(nickname, providerUserId),
        profileImageUrl);
  }

  private String createVirtualEmail(String nickname, String providerUserId) {
    return nickname + "_" + providerUserId + "@" + KAKAO_EMAIL_DOMAIN;
  }

  private String resolveName(String nickname, String providerUserId) {
    if (DEFAULT_NICKNAME.equals(nickname)) {
      return DEFAULT_NICKNAME + "_" + providerUserId;
    }
    return nickname;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object value) {
    if (value instanceof Map<?, ?>) {
      return (Map<String, Object>) value;
    }
    return Map.of();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
