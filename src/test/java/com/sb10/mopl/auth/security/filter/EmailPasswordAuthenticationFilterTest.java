package com.sb10.mopl.auth.security.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.exception.InvalidSignInRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class EmailPasswordAuthenticationFilterTest {

  private static final ValidatorFactory VALIDATOR_FACTORY =
      Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

  @AfterAll
  static void closeValidatorFactory() {
    VALIDATOR_FACTORY.close();
  }

  @Test
  @DisplayName("로그인 요청의 Content-Type이 없으면 잘못된 로그인 요청 예외를 던진다")
  void attemptAuthentication_fail_whenContentTypeIsMissing() {
    EmailPasswordAuthenticationFilter filter = filter(authentication -> authentication);
    MockHttpServletRequest request = signInRequest();

    InvalidSignInRequestException exception =
        assertThrows(
            InvalidSignInRequestException.class,
            () -> filter.attemptAuthentication(request, new MockHttpServletResponse()));

    assertTrue(exception.getDetails().containsKey("contentType"));
  }

  @Test
  @DisplayName("로그인 요청의 Content-Type이 해석할 수 없는 형식이면 잘못된 로그인 요청 예외를 던진다")
  void attemptAuthentication_fail_whenContentTypeIsInvalidMediaType() {
    EmailPasswordAuthenticationFilter filter = filter(authentication -> authentication);
    MockHttpServletRequest request = signInRequest();
    request.setContentType("invalid media type");

    InvalidSignInRequestException exception =
        assertThrows(
            InvalidSignInRequestException.class,
            () -> filter.attemptAuthentication(request, new MockHttpServletResponse()));

    assertTrue(exception.getDetails().containsKey("contentType"));
  }

  @Test
  @DisplayName("로그인 요청 값이 검증 조건을 만족하지 않으면 필드별 오류를 담아 예외를 던진다")
  void attemptAuthentication_fail_whenCredentialsViolateValidation() {
    EmailPasswordAuthenticationFilter filter = filter(authentication -> authentication);
    MockHttpServletRequest request = signInRequest();
    request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    request.addParameter("username", "not-email");
    request.addParameter("password", "");

    InvalidSignInRequestException exception =
        assertThrows(
            InvalidSignInRequestException.class,
            () -> filter.attemptAuthentication(request, new MockHttpServletResponse()));

    Map<String, Object> details = exception.getDetails();
    assertAll(
        () -> assertTrue(details.containsKey("username")),
        () -> assertTrue(details.containsKey("password")));
  }

  @Test
  @DisplayName("같은 필드에 검증 오류가 여러 개 있으면 메시지를 합쳐서 반환한다")
  void attemptAuthentication_fail_whenSameFieldHasMultipleViolations() {
    Validator validator = mock(Validator.class);
    doReturn(Set.of(violation("username", "first"), violation("username", "second")))
        .when(validator)
        .validate(org.mockito.ArgumentMatchers.any());
    EmailPasswordAuthenticationFilter filter =
        new EmailPasswordAuthenticationFilter(
            authentication -> authentication,
            (request, response, authentication) -> {},
            (request, response, exception) -> {},
            validator);
    MockHttpServletRequest request = signInRequest();
    request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    request.addParameter("username", "login-user@example.com");
    request.addParameter("password", "password123");

    InvalidSignInRequestException exception =
        assertThrows(
            InvalidSignInRequestException.class,
            () -> filter.attemptAuthentication(request, new MockHttpServletResponse()));

    assertTrue(exception.getDetails().get("username").toString().contains("; "));
  }

  @Test
  @DisplayName("컨텍스트 경로가 포함된 로그인 요청도 인증 필터가 처리한다")
  void doFilter_success_whenRequestHasContextPath() throws Exception {
    AtomicBoolean authenticationInvoked = new AtomicBoolean(false);
    AtomicBoolean successInvoked = new AtomicBoolean(false);
    EmailPasswordAuthenticationFilter filter =
        filter(
            authentication -> {
              authenticationInvoked.set(true);
              return UsernamePasswordAuthenticationToken.authenticated(
                  authentication.getPrincipal(), authentication.getCredentials(), null);
            });
    filter.setAuthenticationSuccessHandler(
        (request, response, authentication) -> successInvoked.set(true));
    MockHttpServletRequest request = signInRequest();
    request.setContextPath("/mopl");
    request.setRequestURI("/mopl/api/auth/sign-in");
    request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    request.addParameter("username", "login-user@example.com");
    request.addParameter("password", "password123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, (request1, response1) -> chainInvoked.set(true));

    assertAll(
        () -> assertTrue(authenticationInvoked.get()),
        () -> assertTrue(successInvoked.get()),
        () -> assertTrue(!chainInvoked.get()));
  }

  private EmailPasswordAuthenticationFilter filter(AuthenticationManager authenticationManager) {
    return new EmailPasswordAuthenticationFilter(
        authenticationManager,
        (request, response, authentication) -> {},
        (request, response, exception) -> {},
        VALIDATOR);
  }

  private MockHttpServletRequest signInRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/sign-in");
    request.setRequestURI("/api/auth/sign-in");
    return request;
  }

  private ConstraintViolation<com.sb10.mopl.auth.dto.request.SignInRequest> violation(
      String propertyName, String message) {
    @SuppressWarnings("unchecked")
    ConstraintViolation<com.sb10.mopl.auth.dto.request.SignInRequest> violation =
        mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyName);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    return violation;
  }
}
