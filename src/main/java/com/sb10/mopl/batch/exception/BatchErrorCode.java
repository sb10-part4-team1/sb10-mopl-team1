package com.sb10.mopl.batch.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 외부 수집 배치(Batch) 모듈 전용 에러 코드 열거형 클래스입니다. */
@RequiredArgsConstructor
@Getter
public enum BatchErrorCode implements ErrorCode {
  INVALID_API_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "BT01", "외부 API 응답 구조가 변경되었거나 비정상입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
