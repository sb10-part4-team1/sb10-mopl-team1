package com.sb10.mopl.auth.security.handler;

import static com.sb10.mopl.auth.security.RefreshTokenCookieAssertions.expectRefreshTokenCookie;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.auth.entity.SocialAccount;
import com.sb10.mopl.auth.entity.SocialProvider;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.repository.SocialAccountRepository;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Oauth2LoginSuccessHandlerIntegrationTest {

  private static final String GOOGLE_PROVIDER_USER_ID = "google-provider-user-id";
  private static final String GOOGLE_EMAIL = "google-user@example.com";
  private static final String KAKAO_PROVIDER_USER_ID = "123456789";
  private static final String KAKAO_NICKNAME = "kakao-user";

  @Autowired private Oauth2LoginSuccessHandler oauth2LoginSuccessHandler;

  @Autowired private MockMvc mockMvc;

  @Autowired private SocialAccountRepository socialAccountRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private JwtSessionRepository jwtSessionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private JwtProperties jwtProperties;

  @BeforeEach
  void setUp() {
    socialAccountRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    jwtSessionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("Google 소셜 로그인 성공 시 사용자와 소셜 계정을 생성하고 리프레시 토큰 쿠키를 발급한다")
  void login_success_withGoogle_createsUserSocialAccountAndRefreshTokenCookie() throws Exception {
    MockHttpServletResponse response = authenticate(googleAuthentication());

    User user = userRepository.findByEmailAndIsDeletedFalse(GOOGLE_EMAIL).orElseThrow();
    SocialAccount socialAccount = findSocialAccount(SocialProvider.GOOGLE, GOOGLE_PROVIDER_USER_ID);

    assertAll(
        () -> assertEquals("/", response.getRedirectedUrl()),
        () -> assertEquals(user.getId(), socialAccount.getUser().getId()),
        () -> assertEquals("Google User", user.getName()),
        () -> assertEquals("https://example.com/google.png", user.getProfileImageUrl()),
        () -> assertEquals(1L, userRepository.count()),
        () -> assertEquals(1L, socialAccountRepository.count()));

    assertRefreshTokenCookie(response, jwtProperties);
  }

  @Test
  @DisplayName("Kakao 소셜 로그인 성공 시 가상 이메일 규칙으로 사용자와 소셜 계정을 생성한다")
  void login_success_withKakao_createsUserUsingVirtualEmail() throws Exception {
    MockHttpServletResponse response = authenticate(kakaoAuthentication());

    String virtualEmail = KAKAO_NICKNAME + "_" + KAKAO_PROVIDER_USER_ID + "@kakao.com";
    User user = userRepository.findByEmailAndIsDeletedFalse(virtualEmail).orElseThrow();
    SocialAccount socialAccount = findSocialAccount(SocialProvider.KAKAO, KAKAO_PROVIDER_USER_ID);

    assertAll(
        () -> assertEquals("/", response.getRedirectedUrl()),
        () -> assertEquals(KAKAO_NICKNAME, user.getName()),
        () -> assertEquals("https://example.com/kakao.png", user.getProfileImageUrl()),
        () -> assertEquals(user.getId(), socialAccount.getUser().getId()),
        () -> assertEquals(1L, userRepository.count()),
        () -> assertEquals(1L, socialAccountRepository.count()));

    assertRefreshTokenCookie(response, jwtProperties);
  }

  @Test
  @DisplayName("기존 이메일 사용자로 Google 소셜 로그인하면 신규 사용자 없이 소셜 계정을 연동한다")
  void login_success_withExistingEmail_linksSocialAccountToExistingUser() throws Exception {
    User existingUser = saveUser();

    authenticate(googleAuthentication());

    SocialAccount socialAccount = findSocialAccount(SocialProvider.GOOGLE, GOOGLE_PROVIDER_USER_ID);

    assertAll(
        () -> assertEquals(1L, userRepository.count()),
        () -> assertEquals(existingUser.getId(), socialAccount.getUser().getId()),
        () -> assertEquals(GOOGLE_EMAIL, socialAccount.getUser().getEmail()));
  }

  @Test
  @DisplayName("동일한 Google 소셜 계정으로 재로그인하면 사용자와 소셜 계정을 중복 생성하지 않는다")
  void login_success_withExistingSocialAccount_reusesLinkedUser() throws Exception {
    authenticate(googleAuthentication());

    MockHttpServletResponse response = authenticate(googleAuthentication());

    assertAll(
        () -> assertEquals("/", response.getRedirectedUrl()),
        () -> assertEquals(1L, userRepository.count()),
        () -> assertEquals(1L, socialAccountRepository.count()));
    assertRefreshTokenCookie(response, jwtProperties);
  }

  @Test
  @DisplayName("잠긴 소셜 계정은 토큰을 발급하지 않고 로그인 실패 화면으로 리다이렉트한다")
  void login_failure_withLockedSocialAccount_doesNotIssueToken() throws Exception {
    authenticate(googleAuthentication());
    User user = userRepository.findByEmailAndIsDeletedFalse(GOOGLE_EMAIL).orElseThrow();
    user.changeLocked(true);
    userRepository.saveAndFlush(user);
    long refreshTokenCount = refreshTokenRepository.count();

    MockHttpServletResponse response = authenticate(googleAuthentication());

    assertAll(
        () -> assertTrue(response.getRedirectedUrl().startsWith("/#/sign-in?error=oauth_failed")),
        () -> assertEquals("잠긴 계정은 로그인할 수 없습니다.", errorMessage(response)),
        () -> assertEquals(refreshTokenCount, refreshTokenRepository.count()),
        () -> assertNull(response.getCookie(refreshTokenCookieName())));
  }

  @Test
  @DisplayName("이메일이 인증되지 않은 Google 계정은 토큰을 발급하지 않고 로그인 실패 화면으로 리다이렉트한다")
  void login_failure_withUnverifiedGoogleEmail_doesNotIssueToken() throws Exception {
    MockHttpServletResponse response = authenticate(googleAuthentication(false));

    assertAll(
        () -> assertTrue(response.getRedirectedUrl().startsWith("/#/sign-in?error=oauth_failed")),
        () -> assertEquals("Google 계정의 이메일 인증이 필요합니다.", errorMessage(response)),
        () -> assertEquals(0L, userRepository.count()),
        () -> assertEquals(0L, socialAccountRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()),
        () -> assertNull(response.getCookie(refreshTokenCookieName())));
  }

  @Test
  @DisplayName("소셜 로그인으로 발급된 리프레시 토큰 쿠키로 JwtDto를 재발급받는다")
  void refresh_success_withSocialLoginRefreshToken_returnsJwtDto() throws Exception {
    MockHttpServletResponse socialLoginResponse = authenticate(googleAuthentication());
    Cookie refreshToken = socialLoginResponse.getCookie(refreshTokenCookieName());

    assertNotNull(refreshToken);

    expectRefreshTokenCookie(
            mockMvc
                .perform(post("/api/auth/refresh").cookie(refreshToken).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userDto.email").value(GOOGLE_EMAIL))
                .andExpect(jsonPath("$.userDto.name").value("Google User"))
                .andExpect(jsonPath("$.accessToken").isString()),
            jwtProperties)
        .andReturn();
  }

  private MockHttpServletResponse authenticate(OAuth2AuthenticationToken authentication)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    oauth2LoginSuccessHandler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication);
    return response;
  }

  private OAuth2AuthenticationToken googleAuthentication() {
    return googleAuthentication(true);
  }

  private OAuth2AuthenticationToken googleAuthentication(boolean emailVerified) {
    return authentication(
        "google",
        "sub",
        Map.of(
            "sub",
            GOOGLE_PROVIDER_USER_ID,
            "email",
            GOOGLE_EMAIL,
            "email_verified",
            emailVerified,
            "name",
            "Google User",
            "picture",
            "https://example.com/google.png"));
  }

  private OAuth2AuthenticationToken kakaoAuthentication() {
    return authentication(
        "kakao",
        "id",
        Map.of(
            "id",
            KAKAO_PROVIDER_USER_ID,
            "kakao_account",
            Map.of(
                "profile",
                Map.of(
                    "nickname",
                    KAKAO_NICKNAME,
                    "profile_image_url",
                    "https://example.com/kakao.png"))));
  }

  private OAuth2AuthenticationToken authentication(
      String registrationId, String nameAttributeKey, Map<String, Object> attributes) {
    DefaultOAuth2User principal =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, nameAttributeKey);
    return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId);
  }

  private SocialAccount findSocialAccount(SocialProvider provider, String providerUserId) {
    return socialAccountRepository
        .findByProviderAndProviderUserIdWithUser(provider, providerUserId)
        .orElseThrow();
  }

  private User saveUser() {
    return userRepository.saveAndFlush(
        User.createUser("existing-user", GOOGLE_EMAIL, "password", null));
  }

  private String refreshTokenCookieName() {
    return jwtProperties.refreshTokenCookie().name();
  }

  private String errorMessage(MockHttpServletResponse response) {
    String redirectUrl = response.getRedirectedUrl();
    assertNotNull(redirectUrl);
    String encodedMessage = redirectUrl.substring(redirectUrl.indexOf("error_message=") + 14);
    return URLDecoder.decode(encodedMessage, StandardCharsets.UTF_8);
  }

  private void assertRefreshTokenCookie(
      MockHttpServletResponse response, JwtProperties properties) {
    Cookie refreshToken = response.getCookie(properties.refreshTokenCookie().name());

    assertAll(
        () -> assertNotNull(refreshToken),
        () -> assertTrue(!refreshToken.getValue().isBlank()),
        () -> assertEquals(properties.refreshTokenCookie().path(), refreshToken.getPath()),
        () -> assertEquals(properties.refreshTokenCookie().httpOnly(), refreshToken.isHttpOnly()),
        () -> assertEquals(properties.refreshTokenCookie().secure(), refreshToken.getSecure()));
  }
}
