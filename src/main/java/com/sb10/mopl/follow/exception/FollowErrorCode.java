package com.sb10.mopl.follow.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements ErrorCode {
  FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FL01", "팔로우를 찾을 수 없습니다."),
  FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "FL02", "이미 팔로우 하였습니다."),
  UNAUTHORIZED_FOLLOW_ACCESS(HttpStatus.FORBIDDEN, "FL03", "팔로우에 접근할 권한이 없습니다."),
  INVALID_FOLLOW_VALUE(HttpStatus.BAD_REQUEST, "FL04", "올바르지 않은 팔로우 값입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
