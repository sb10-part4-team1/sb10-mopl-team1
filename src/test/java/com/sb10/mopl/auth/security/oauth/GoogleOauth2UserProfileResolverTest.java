package com.sb10.mopl.auth.security.oauth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sb10.mopl.auth.entity.SocialProvider;
import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleOauth2UserProfileResolverTest {

  private final GoogleOauth2UserProfileResolver resolver = new GoogleOauth2UserProfileResolver();

  @Test
  @DisplayName("provider()는 GOOGLE을 반환한다")
  void provider_returnsGoogle() {
    // when & then
    assertEquals(SocialProvider.GOOGLE, resolver.provider());
  }

  @Test
  @DisplayName("이메일 인증이 완료된 속성이면 프로필을 정상적으로 생성한다")
  void resolve_success_whenAttributesAreComplete() {
    // given
    Map<String, Object> attributes =
        googleAttributes("google-sub", "user@example.com", "구글유저", true);

    // when
    Oauth2UserProfile profile = resolver.resolve(attributes);

    // then
    assertAll(
        () -> assertEquals(SocialProvider.GOOGLE, profile.provider()),
        () -> assertEquals("google-sub", profile.providerUserId()),
        () -> assertEquals("user@example.com", profile.email()),
        () -> assertEquals("구글유저", profile.name()),
        () -> assertEquals("https://example.com/picture.png", profile.profileImageUrl()));
  }

  @Test
  @DisplayName("name이 blank이면 email로 대체한다")
  void resolve_usesEmailAsName_whenNameIsBlank() {
    // given
    Map<String, Object> attributes = googleAttributes("google-sub", "user@example.com", " ", true);

    // when
    Oauth2UserProfile profile = resolver.resolve(attributes);

    // then
    assertEquals("user@example.com", profile.name());
  }

  @Test
  @DisplayName("이메일 인증이 완료되지 않으면 인증 실패 예외를 발생시킨다")
  void resolve_throwsAuthenticationFailed_whenEmailIsNotVerified() {
    // given
    Map<String, Object> attributes =
        googleAttributes("google-sub", "user@example.com", "구글유저", false);

    // when
    MoplException exception = assertThrows(MoplException.class, () -> resolver.resolve(attributes));

    // then
    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
  }

  @Test
  @DisplayName("email_verified 속성이 없으면 인증 실패 예외를 발생시킨다")
  void resolve_throwsAuthenticationFailed_whenEmailVerifiedAttributeIsMissing() {
    // given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("sub", "google-sub");
    attributes.put("email", "user@example.com");
    attributes.put("name", "구글유저");
    attributes.put("picture", "https://example.com/picture.png");

    // when & then
    assertThrows(MoplException.class, () -> resolver.resolve(attributes));
  }

  private Map<String, Object> googleAttributes(
      String sub, String email, String name, boolean emailVerified) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("sub", sub);
    attributes.put("email", email);
    attributes.put("name", name);
    attributes.put("picture", "https://example.com/picture.png");
    attributes.put("email_verified", emailVerified);
    return attributes;
  }
}
