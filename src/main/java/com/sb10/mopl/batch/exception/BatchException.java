package com.sb10.mopl.batch.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

/** 배치 모듈 실행 과정에서 발생하는 시스템 및 인프라 예외를 정의하는 커스텀 예외 클래스입니다. */
public class BatchException extends MoplException {

  /**
   * 에러 코드와 상세 정보를 지정하여 배치 예외를 생성합니다.
   *
   * @param errorCode 배치 전용 에러 코드
   * @param details 에러 관련 상세 컨텍스트 정보 Map
   */
  public BatchException(BatchErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  /**
   * 에러 코드, 상세 정보 및 원인 예외를 지정하여 배치 예외를 생성합니다.
   *
   * @param errorCode 배치 전용 에러 코드
   * @param details 에러 관련 상세 컨텍스트 정보 Map
   * @param cause 상위 전파를 위해 보존할 예외 원인
   */
  public BatchException(BatchErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
