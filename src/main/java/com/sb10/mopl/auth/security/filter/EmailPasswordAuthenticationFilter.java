package com.sb10.mopl.auth.security.filter;

import com.sb10.mopl.auth.dto.request.SignInRequest;
import com.sb10.mopl.auth.exception.InvalidSignInRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class EmailPasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private static final RequestMatcher SIGN_IN_REQUEST_MATCHER =
      request -> "POST".equals(request.getMethod()) && "/api/auth/sign-in".equals(path(request));

  private final Validator validator;

  public EmailPasswordAuthenticationFilter(
      AuthenticationManager authenticationManager,
      AuthenticationSuccessHandler authenticationSuccessHandler,
      AuthenticationFailureHandler authenticationFailureHandler,
      Validator validator) {
    this.validator = validator;
    setAuthenticationManager(authenticationManager);
    setAuthenticationSuccessHandler(authenticationSuccessHandler);
    setAuthenticationFailureHandler(authenticationFailureHandler);
    setRequiresAuthenticationRequestMatcher(SIGN_IN_REQUEST_MATCHER);
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    if (!isFormUrlEncoded(request)) {
      throw new InvalidSignInRequestException(
          Map.of("contentType", "application/x-www-form-urlencoded 형식이어야 합니다."));
    }

    SignInRequest signInRequest =
        new SignInRequest(obtainUsername(request), obtainPassword(request));
    validate(signInRequest);

    UsernamePasswordAuthenticationToken authenticationRequest =
        UsernamePasswordAuthenticationToken.unauthenticated(
            signInRequest.username(), signInRequest.password());
    setDetails(request, authenticationRequest);
    return getAuthenticationManager().authenticate(authenticationRequest);
  }

  private boolean isFormUrlEncoded(HttpServletRequest request) {
    String contentType = request.getContentType();
    if (contentType == null || contentType.isBlank()) {
      return false;
    }
    try {
      return MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(
          MediaType.parseMediaType(contentType));
    } catch (InvalidMediaTypeException exception) {
      return false;
    }
  }

  private static String path(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath == null || contextPath.isBlank()) {
      return requestUri;
    }
    return requestUri.substring(contextPath.length());
  }

  private void validate(SignInRequest signInRequest) {
    Set<ConstraintViolation<SignInRequest>> violations = validator.validate(signInRequest);
    if (violations.isEmpty()) {
      return;
    }

    Map<String, Object> details = new HashMap<>();
    for (ConstraintViolation<SignInRequest> violation : violations) {
      details.merge(
          violation.getPropertyPath().toString(),
          violation.getMessage(),
          (existing, incoming) -> existing + "; " + incoming);
    }
    throw new InvalidSignInRequestException(details);
  }
}
