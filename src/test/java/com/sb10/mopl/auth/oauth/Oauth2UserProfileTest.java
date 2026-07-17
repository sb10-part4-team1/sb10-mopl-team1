package com.sb10.mopl.auth.oauth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sb10.mopl.auth.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Oauth2UserProfileTest {

  @Test
  @DisplayName("필수 필드가 모두 유효하면 정상 생성되고 profileImageUrl은 null도 허용한다")
  void constructor_success_whenRequiredFieldsAreValid() {
    // given & when
    Oauth2UserProfile profile =
        new Oauth2UserProfile(
            SocialProvider.GOOGLE, "provider-user-id", "user@example.com", "name", null);

    // then
    assertAll(
        () -> assertEquals(SocialProvider.GOOGLE, profile.provider()),
        () -> assertEquals("provider-user-id", profile.providerUserId()),
        () -> assertEquals("user@example.com", profile.email()),
        () -> assertEquals("name", profile.name()),
        () -> assertEquals(null, profile.profileImageUrl()));
  }

  @Test
  @DisplayName("provider가 null이면 예외를 발생시킨다")
  void constructor_throws_whenProviderIsNull() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> new Oauth2UserProfile(null, "provider-user-id", "user@example.com", "name", null));
  }

  @Test
  @DisplayName("providerUserId가 blank이면 예외를 발생시킨다")
  void constructor_throws_whenProviderUserIdIsBlank() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> new Oauth2UserProfile(SocialProvider.GOOGLE, " ", "user@example.com", "name", null));
  }

  @Test
  @DisplayName("email이 blank이면 예외를 발생시킨다")
  void constructor_throws_whenEmailIsBlank() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> new Oauth2UserProfile(SocialProvider.GOOGLE, "provider-user-id", " ", "name", null));
  }

  @Test
  @DisplayName("name이 blank이면 예외를 발생시킨다")
  void constructor_throws_whenNameIsBlank() {
    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Oauth2UserProfile(
                SocialProvider.GOOGLE, "provider-user-id", "user@example.com", " ", null));
  }
}
