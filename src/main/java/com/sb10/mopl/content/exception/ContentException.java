package com.sb10.mopl.content.exception;

import com.sb10.mopl.exception.MoplException;
import java.util.Map;

public class ContentException extends MoplException {

  /**
   * 에러 코드와 상세 정보를 지정하여 예외를 생성합니다.
   *
   * @param errorCode 콘텐츠 전용 에러 코드
   * @param details 에러 관련 상세 컨텍스트 정보 Map
   */
  public ContentException(ContentErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  /**
   * 에러 코드, 상세 정보 및 원인 예외를 지정하여 예외를 생성합니다.
   *
   * @param errorCode 콘텐츠 전용 에러 코드
   * @param details 에러 관련 상세 컨텍스트 정보 Map
   * @param cause 상위 전파를 위해 보존할 예외 원인
   */
  public ContentException(
      ContentErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
