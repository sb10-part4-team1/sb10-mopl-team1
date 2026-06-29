package com.sb10.mopl.content.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ContentErrorCode implements ErrorCode {
  CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CT01", "콘텐츠를 찾을 수 없습니다."),
  INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "CT02", "올바르지 않은 콘텐츠 형식입니다."),
  UNAUTHORIZED_CONTENT_ACCESS(HttpStatus.FORBIDDEN, "CT03", "콘텐츠에 접근할 권한이 없습니다."),
  INVALID_CONTENT_DATA(HttpStatus.BAD_REQUEST, "CT04", "올바르지 않은 콘텐츠 데이터입니다."),
  DUPLICATE_TAG_NAME(HttpStatus.CONFLICT, "CT05", "이미 존재하는 태그명입니다."),
  INVALID_CURSOR_VALUE(HttpStatus.BAD_REQUEST, "CT06", "올바르지 않은 커서 형식입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
