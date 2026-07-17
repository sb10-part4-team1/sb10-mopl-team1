package com.sb10.mopl.watchingsession.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum WatchingSessionErrorCode implements ErrorCode {
  INVALID_CURSOR_VALUE(HttpStatus.BAD_REQUEST, "WS01", "올바르지 않은 커서 형식입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
