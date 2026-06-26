package com.sb10.mopl.auth.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AuthErrorCode implements ErrorCode {
  AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH01", "인증에 실패했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
