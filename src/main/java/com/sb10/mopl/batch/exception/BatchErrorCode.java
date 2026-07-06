package com.sb10.mopl.batch.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 외부 수집 배치(Batch) 모듈 전용 에러 코드 열거형 클래스입니다. */
@RequiredArgsConstructor
@Getter
public enum BatchErrorCode implements ErrorCode {
  INVALID_API_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "BT01", "외부 API 응답 구조가 변경되었거나 비정상입니다."),
  JOB_NOT_FOUND(HttpStatus.BAD_REQUEST, "BT02", "존재하지 않거나 알 수 없는 배치 Job 이름입니다."),
  JOB_RESTART_UNAVAILABLE(HttpStatus.BAD_REQUEST, "BT03", "재시작 가능한 FAILED 상태의 배치 이력이 존재하지 않습니다."),
  JOB_ALREADY_RUNNING(HttpStatus.CONFLICT, "BT04", "해당 배치 Job이 현재 실행 중이므로 진행할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
