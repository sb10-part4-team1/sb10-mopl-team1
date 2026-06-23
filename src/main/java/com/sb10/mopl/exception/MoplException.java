package com.sb10.mopl.exception;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;

/**
 * MOPL 프로젝트의 최상위 커스텀 예외 클래스입니다. 도메인별 예외 상황을 일관되게 표현하며, 전역 예외 핸들러에서 이를 감지하여 정형화된 JSON 응답으로 반환합니다.
 */
@Getter
public class MoplException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  /**
   * @param errorCode 구현체 에러 코드
   * @param details 에러에 대한 컨텍스트 및 상세 데이터 Map
   */
  public MoplException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.details = details != null ? Map.copyOf(details) : Collections.emptyMap();
  }

  /**
   * @param errorCode 구현체 에러 코드
   * @param details 에러에 대한 컨텍스트 및 상세 데이터 Map
   * @param cause 상위 전파를 위해 보존할 예외 원인
   */
  public MoplException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
    this.details = details != null ? Map.copyOf(details) : Collections.emptyMap();
  }
}
