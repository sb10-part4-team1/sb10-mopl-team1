package com.sb10.mopl.review.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

  REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND,"RV01", "리뷰를 찾을 수 없습니다."),
  REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT,"RV02", "이미 해당 콘텐츠에 리뷰를 작성했습니다."),
  UNAUTHORIZED_REVIEW_ACCESS(HttpStatus.FORBIDDEN,"RV03", "리뷰에 접근할 권한이 없습니다."),
  INVALID_REVIEW_VALUE(HttpStatus.BAD_REQUEST, "RV04", "올바르지 않은 리뷰 값입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
