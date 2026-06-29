package com.sb10.mopl.auth.security.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.user.entity.UserRole;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserArgumentResolverTest {

  private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("현재 사용자 애너테이션이 붙은 인증 사용자 파라미터만 지원한다")
  void supportsParameter_success_whenCurrentUserAuthenticatedUser() throws Exception {
    assertTrue(
        resolver.supportsParameter(parameter("requiredCurrentUser", AuthenticatedUser.class)));
    assertFalse(resolver.supportsParameter(parameter("unsupportedType", String.class)));
    assertFalse(
        resolver.supportsParameter(parameter("plainAuthenticatedUser", AuthenticatedUser.class)));
  }

  @Test
  @DisplayName("보안 컨텍스트에서 현재 사용자 정보를 조회한다")
  void resolveArgument_success_whenAuthenticationHasAuthenticatedUserPrincipal() throws Exception {
    AuthenticatedUser currentUser =
        new AuthenticatedUser(UUID.randomUUID(), "current-user@example.com", UserRole.USER);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                currentUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    Object resolved =
        resolver.resolveArgument(
            parameter("requiredCurrentUser", AuthenticatedUser.class), null, null, null);

    assertSame(currentUser, resolved);
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) resolved;
    assertEquals(currentUser.id(), authenticatedUser.id());
    assertEquals("current-user@example.com", authenticatedUser.email());
    assertEquals(UserRole.USER, authenticatedUser.role());
  }

  @Test
  @DisplayName("선택적 현재 사용자는 인증 정보가 없으면 값 없음을 반환한다")
  void resolveArgument_success_whenCurrentUserIsOptionalAndAuthenticationIsMissing()
      throws Exception {
    Object resolved =
        resolver.resolveArgument(
            parameter("optionalCurrentUser", AuthenticatedUser.class), null, null, null);

    assertNull(resolved);
  }

  @Test
  @DisplayName("필수 현재 사용자는 인증 정보가 없으면 인증 예외를 던진다")
  void resolveArgument_fail_whenAuthenticationIsMissing() throws Exception {
    MoplException exception =
        assertThrows(
            MoplException.class,
            () ->
                resolver.resolveArgument(
                    parameter("requiredCurrentUser", AuthenticatedUser.class), null, null, null));

    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    assertEquals("인증이 필요합니다.", exception.getDetails().get("message"));
  }

  @Test
  @DisplayName("인증 객체가 있지만 인증되지 않은 상태이면 인증 예외를 던진다")
  void resolveArgument_fail_whenAuthenticationIsNotAuthenticated() throws Exception {
    AuthenticatedUser currentUser =
        new AuthenticatedUser(UUID.randomUUID(), "current-user@example.com", UserRole.USER);
    SecurityContextHolder.getContext()
        .setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated(currentUser, null));

    MoplException exception =
        assertThrows(
            MoplException.class,
            () ->
                resolver.resolveArgument(
                    parameter("requiredCurrentUser", AuthenticatedUser.class), null, null, null));

    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    assertEquals("인증이 필요합니다.", exception.getDetails().get("message"));
  }

  @Test
  @DisplayName("인증 principal이 현재 사용자 타입이 아니면 인증 예외를 던진다")
  void resolveArgument_fail_whenPrincipalIsNotAuthenticatedUser() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "anonymous", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    MoplException exception =
        assertThrows(
            MoplException.class,
            () ->
                resolver.resolveArgument(
                    parameter("requiredCurrentUser", AuthenticatedUser.class), null, null, null));

    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    assertEquals("인증이 필요합니다.", exception.getDetails().get("message"));
  }

  @Test
  @DisplayName("선택 현재 사용자는 principal 타입이 맞지 않으면 값을 반환하지 않는다")
  void resolveArgument_success_whenOptionalPrincipalIsNotAuthenticatedUser() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "anonymous", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    Object resolved =
        resolver.resolveArgument(
            parameter("optionalCurrentUser", AuthenticatedUser.class), null, null, null);

    assertNull(resolved);
  }

  private MethodParameter parameter(String methodName, Class<?> parameterType) throws Exception {
    Method method = TestController.class.getDeclaredMethod(methodName, parameterType);
    return new MethodParameter(method, 0);
  }

  private static class TestController {

    void requiredCurrentUser(@CurrentUser AuthenticatedUser currentUser) {}

    void optionalCurrentUser(@CurrentUser(required = false) AuthenticatedUser currentUser) {}

    void unsupportedType(@CurrentUser String currentUser) {}

    void plainAuthenticatedUser(AuthenticatedUser currentUser) {}
  }
}
