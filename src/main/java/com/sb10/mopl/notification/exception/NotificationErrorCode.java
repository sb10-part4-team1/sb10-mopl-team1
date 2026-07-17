package com.sb10.mopl.notification.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum NotificationErrorCode implements ErrorCode {
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT01", "알림을 찾을 수 없습니다."),
  NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NT02", "본인의 알림만 처리할 수 있습니다."),
  INVALID_CURSOR_VALUE(HttpStatus.BAD_REQUEST, "NT03", "올바르지 않은 커서 형식입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
