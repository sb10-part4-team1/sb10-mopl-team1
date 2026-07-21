package com.sb10.mopl.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialProviderTest {

  @Test
  @DisplayName("소셜 제공자 - registrationId로 변환 성공 - google")
  void fromRegistrationId_success_google() {
    // when & then
    assertThat(SocialProvider.fromRegistrationId("google")).isEqualTo(SocialProvider.GOOGLE);
  }

  @Test
  @DisplayName("소셜 제공자 - registrationId로 변환 성공 - kakao")
  void fromRegistrationId_success_kakao() {
    // when & then
    assertThat(SocialProvider.fromRegistrationId("kakao")).isEqualTo(SocialProvider.KAKAO);
  }

  @Test
  @DisplayName("소셜 제공자 - registrationId 대소문자 혼용도 변환 성공")
  void fromRegistrationId_success_caseInsensitive() {
    // when & then
    assertThat(SocialProvider.fromRegistrationId("GOOGLE")).isEqualTo(SocialProvider.GOOGLE);
    assertThat(SocialProvider.fromRegistrationId("KaKaO")).isEqualTo(SocialProvider.KAKAO);
  }

  @Test
  @DisplayName("소셜 제공자 - registrationId가 null이면 예외 발생")
  void fromRegistrationId_fail_null() {
    // when & then
    assertThatThrownBy(() -> SocialProvider.fromRegistrationId(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("소셜 제공자 - registrationId가 빈 문자열이면 예외 발생")
  void fromRegistrationId_fail_empty() {
    // when & then
    assertThatThrownBy(() -> SocialProvider.fromRegistrationId(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("소셜 제공자 - registrationId가 공백이면 예외 발생")
  void fromRegistrationId_fail_blank() {
    // when & then
    assertThatThrownBy(() -> SocialProvider.fromRegistrationId(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("소셜 제공자 - 지원하지 않는 registrationId면 예외 발생")
  void fromRegistrationId_fail_unsupported() {
    // when & then
    assertThatThrownBy(() -> SocialProvider.fromRegistrationId("naver"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("소셜 제공자 - registrationId 역변환 성공")
  void registrationId_success() {
    // when & then
    assertThat(SocialProvider.GOOGLE.registrationId()).isEqualTo("google");
    assertThat(SocialProvider.KAKAO.registrationId()).isEqualTo("kakao");
  }
}
