package com.sb10.mopl.auth.security.provider;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.entity.TemporaryPassword;
import com.sb10.mopl.auth.repository.TemporaryPasswordRepository;
import com.sb10.mopl.auth.security.principal.MoplUserDetails;
import com.sb10.mopl.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordAuthenticationProviderTest {

  private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
  private static final String EMAIL = "user@example.com";
  private static final String RAW_PASSWORD = "Temp1234!";
  private static final String ENCODED_PASSWORD = "encoded-temporary-password";

  @Mock private TemporaryPasswordRepository temporaryPasswordRepository;

  @Mock private PasswordEncoder passwordEncoder;

  private TemporaryPasswordAuthenticationProvider provider;

  @BeforeEach
  void setUp() {
    provider =
        new TemporaryPasswordAuthenticationProvider(
            temporaryPasswordRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("자격 증명이 유효하면 인증된 토큰을 반환한다")
  void authenticate_success_whenCredentialsAreValid() {
    // given
    User user = user(false);
    TemporaryPassword temporaryPassword = temporaryPassword(user, NOW.plusSeconds(60));
    when(temporaryPasswordRepository.findByUserEmailWithUser(EMAIL))
        .thenReturn(Optional.of(temporaryPassword));
    when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

    // when
    Authentication authenticated = provider.authenticate(authenticationRequest());

    // then
    MoplUserDetails principal = (MoplUserDetails) authenticated.getPrincipal();
    assertAll(
        () -> assertTrue(authenticated.isAuthenticated()),
        () -> assertEquals(EMAIL, principal.getEmail()));
  }

  @Test
  @DisplayName("임시 비밀번호가 조회되지 않으면 잘못된 자격 증명 예외를 발생시킨다")
  void authenticate_throwsBadCredentials_whenTemporaryPasswordNotFound() {
    // given
    when(temporaryPasswordRepository.findByUserEmailWithUser(EMAIL)).thenReturn(Optional.empty());

    // when & then
    assertThrows(
        BadCredentialsException.class, () -> provider.authenticate(authenticationRequest()));
  }

  @Test
  @DisplayName("사용자가 잠겨 있으면 계정 잠김 예외를 발생시킨다")
  void authenticate_throwsLockedException_whenUserIsLocked() {
    // given
    User user = user(true);
    TemporaryPassword temporaryPassword = temporaryPassword(user, NOW.plusSeconds(60));
    when(temporaryPasswordRepository.findByUserEmailWithUser(EMAIL))
        .thenReturn(Optional.of(temporaryPassword));

    // when & then
    assertThrows(LockedException.class, () -> provider.authenticate(authenticationRequest()));
  }

  @Test
  @DisplayName("임시 비밀번호가 만료되었으면 잘못된 자격 증명 예외를 발생시킨다")
  void authenticate_throwsBadCredentials_whenTemporaryPasswordIsExpired() {
    // given
    User user = user(false);
    TemporaryPassword temporaryPassword = temporaryPassword(user, NOW.minusSeconds(1));
    when(temporaryPasswordRepository.findByUserEmailWithUser(EMAIL))
        .thenReturn(Optional.of(temporaryPassword));

    // when & then
    assertThrows(
        BadCredentialsException.class, () -> provider.authenticate(authenticationRequest()));
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 잘못된 자격 증명 예외를 발생시킨다")
  void authenticate_throwsBadCredentials_whenPasswordDoesNotMatch() {
    // given
    User user = user(false);
    TemporaryPassword temporaryPassword = temporaryPassword(user, NOW.plusSeconds(60));
    when(temporaryPasswordRepository.findByUserEmailWithUser(EMAIL))
        .thenReturn(Optional.of(temporaryPassword));
    when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

    // when & then
    assertThrows(
        BadCredentialsException.class, () -> provider.authenticate(authenticationRequest()));
  }

  @Test
  @DisplayName("supports()는 UsernamePasswordAuthenticationToken만 지원한다")
  void supports_returnsTrueOnlyForUsernamePasswordAuthenticationToken() {
    // when & then
    assertAll(
        () -> assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class)),
        () -> assertFalse(provider.supports(Authentication.class)));
  }

  private Authentication authenticationRequest() {
    return new UsernamePasswordAuthenticationToken(EMAIL, RAW_PASSWORD);
  }

  private User user(boolean locked) {
    User user = User.createUser("temp-user", EMAIL, "encoded-password", null);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(user, "isLocked", locked);
    return user;
  }

  private TemporaryPassword temporaryPassword(User user, Instant expiresAt) {
    return TemporaryPassword.create(user, ENCODED_PASSWORD, expiresAt);
  }
}
