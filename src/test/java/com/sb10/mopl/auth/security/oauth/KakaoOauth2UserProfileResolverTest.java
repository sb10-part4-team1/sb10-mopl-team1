package com.sb10.mopl.auth.security.oauth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sb10.mopl.auth.entity.SocialProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoOauth2UserProfileResolverTest {

  private final KakaoOauth2UserProfileResolver resolver = new KakaoOauth2UserProfileResolver();

  @Test
  @DisplayName("provider()는 KAKAO를 반환한다")
  void provider_returnsKakao() {
    // when & then
    assertEquals(SocialProvider.KAKAO, resolver.provider());
  }

  @Test
  @DisplayName("kakao_account.profile에 nickname과 profile_image_url이 있으면 이를 우선 사용한다")
  void resolve_success_whenNicknameAndProfileImageInAccountProfile() {
    // given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 12345L);
    attributes.put(
        "kakao_account",
        Map.of(
            "profile",
            Map.of(
                "nickname", "카카오유저",
                "profile_image_url", "https://example.com/kakao.png")));
    attributes.put(
        "properties",
        Map.of("nickname", "프로퍼티닉네임", "profile_image", "https://example.com/props.png"));

    // when
    Oauth2UserProfile profile = resolver.resolve(attributes);

    // then
    assertAll(
        () -> assertEquals(SocialProvider.KAKAO, profile.provider()),
        () -> assertEquals("12345", profile.providerUserId()),
        () -> assertEquals("카카오유저_12345@kakao.com", profile.email()),
        () -> assertEquals("카카오유저", profile.name()),
        () -> assertEquals("https://example.com/kakao.png", profile.profileImageUrl()));
  }

  @Test
  @DisplayName("kakao_account.profile에 nickname이 없으면 properties.nickname으로 대체한다")
  void resolve_fallsBackToPropertiesNickname_whenAccountProfileNicknameMissing() {
    // given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 12345L);
    attributes.put("kakao_account", Map.of("profile", Map.of()));
    attributes.put("properties", Map.of("nickname", "프로퍼티닉네임"));

    // when
    Oauth2UserProfile profile = resolver.resolve(attributes);

    // then
    assertAll(
        () -> assertEquals("프로퍼티닉네임", profile.name()),
        () -> assertEquals("프로퍼티닉네임_12345@kakao.com", profile.email()));
  }

  @Test
  @DisplayName("nickname을 어디에서도 찾을 수 없으면 기본 닉네임으로 가상 이메일과 이름을 생성한다")
  void resolve_usesDefaultNickname_whenNicknameNotFoundAnywhere() {
    // given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 12345L);

    // when
    Oauth2UserProfile profile = resolver.resolve(attributes);

    // then
    assertAll(
        () -> assertEquals("kakao_12345", profile.name()),
        () -> assertEquals("kakao_12345@kakao.com", profile.email()),
        () -> assertNull(profile.profileImageUrl()));
  }

  @Test
  @DisplayName("kakao_account.profile에 profile_image_url이 없으면 properties.profile_image로 대체한다")
  void resolve_fallsBackToPropertiesProfileImage_whenAccountProfileImageMissing() {
    // given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 12345L);
    attributes.put("kakao_account", Map.of("profile", Map.of("nickname", "카카오유저")));
    attributes.put("properties", Map.of("profile_image", "https://example.com/props.png"));

    // when
    Oauth2UserProfile profile = resolver.resolve(attributes);

    // then
    assertEquals("https://example.com/props.png", profile.profileImageUrl());
  }
}
