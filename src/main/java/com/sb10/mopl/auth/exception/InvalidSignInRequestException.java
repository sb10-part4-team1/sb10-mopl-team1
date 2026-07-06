package com.sb10.mopl.auth.exception;

import java.util.Map;
import org.springframework.security.core.AuthenticationException;

public class InvalidSignInRequestException extends AuthenticationException {

  private final Map<String, Object> details;

  public InvalidSignInRequestException(Map<String, Object> details) {
    super("Invalid sign-in request");
    this.details = details;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
