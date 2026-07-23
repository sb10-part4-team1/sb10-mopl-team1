package com.sb10.mopl.auth.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.entity.SocialProvider;
import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Oauth2UserProfileResolverCompositeTest {

  @Test
  @DisplayName("provider에 해당하는 resolver가 등록되어 있으면 해당 resolver로 위임한다")
  void resolve_delegatesToMatchingResolver_whenProviderIsRegistered() {
    // given
    Oauth2UserProfileResolver googleResolver = mock(Oauth2UserProfileResolver.class);
    Oauth2UserProfileResolver kakaoResolver = mock(Oauth2UserProfileResolver.class);
    when(googleResolver.provider()).thenReturn(SocialProvider.GOOGLE);
    when(kakaoResolver.provider()).thenReturn(SocialProvider.KAKAO);
    Map<String, Object> attributes = Map.of("sub", "id");
    Oauth2UserProfile expectedProfile =
        new Oauth2UserProfile(SocialProvider.GOOGLE, "id", "user@example.com", "name", null);
    when(googleResolver.resolve(attributes)).thenReturn(expectedProfile);

    Oauth2UserProfileResolverComposite composite =
        new Oauth2UserProfileResolverComposite(List.of(googleResolver, kakaoResolver));

    // when
    Oauth2UserProfile profile = composite.resolve(SocialProvider.GOOGLE, attributes);

    // then
    assertEquals(expectedProfile, profile);
    verify(googleResolver).resolve(attributes);
  }

  @Test
  @DisplayName("등록되지 않은 provider를 요청하면 인증 실패 예외를 발생시킨다")
  void resolve_throwsAuthenticationFailed_whenNoResolverIsRegisteredForProvider() {
    // given
    Oauth2UserProfileResolver googleResolver = mock(Oauth2UserProfileResolver.class);
    when(googleResolver.provider()).thenReturn(SocialProvider.GOOGLE);
    Oauth2UserProfileResolverComposite composite =
        new Oauth2UserProfileResolverComposite(List.of(googleResolver));
    Map<String, Object> attributes = Map.of("id", "id");

    // when
    MoplException exception =
        assertThrows(
            MoplException.class, () -> composite.resolve(SocialProvider.KAKAO, attributes));

    // then
    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
  }
}
