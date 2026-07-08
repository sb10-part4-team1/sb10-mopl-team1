package com.sb10.mopl.user.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCode {
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U01", "유저를 찾을 수 없습니다."),
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U02", "이미 사용 중인 이메일입니다."),
  USER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "U03", "해당 사용자에 접근할 권한이 없습니다."),
  INVALID_USER_VALUE(HttpStatus.BAD_REQUEST, "U04", "올바르지 않은 사용자 요청입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
