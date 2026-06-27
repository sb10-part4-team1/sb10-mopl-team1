package com.sb10.mopl.auth.security.user;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUser.class)
        && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    CurrentUser currentUser = parameter.getParameterAnnotation(CurrentUser.class);

    if (authentication == null || !authentication.isAuthenticated()) {
      return handleMissingAuthentication(currentUser);
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser;
    }

    return handleMissingAuthentication(currentUser);
  }

  private Object handleMissingAuthentication(CurrentUser currentUser) {
    if (currentUser != null && !currentUser.required()) {
      return null;
    }
    throw new MoplException(
        AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", "Authentication is required."));
  }
}
