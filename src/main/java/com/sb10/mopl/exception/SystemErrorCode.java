package com.sb10.mopl.exception;

import org.springframework.http.HttpStatus;

/** 특정 도메인에 속하지 않고, 스프링 프레임워크나 자바 런타임 자체에서 발생하는 공통 시스템 에러 코드를 정의한 Enum 클래스입니다. */
public enum SystemErrorCode implements ErrorCode {
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "SYS01", "올바르지 않은 입력값입니다."),
  INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "SYS02", "올바르지 않은 요청 본문(JSON) 형식입니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "SYS03", "지원하지 않는 HTTP 메서드입니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "SYS04", "접근 권한이 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "SYS05", "존재하지 않는 경로 또는 리소스입니다."),
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "SYS99", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  SystemErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
