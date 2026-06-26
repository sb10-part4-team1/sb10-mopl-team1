package com.sb10.mopl.common.exception;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 애플리케이션 전역에서 발생하는 예외를 가로채서 처리하는 컨트롤러 어드바이스 클래스입니다. MoplException을 비롯하여 프레임워크 수준의 바인딩 에러, JSON 형식
 * 에러, 지원하지 않는 HTTP 메서드 호출, 보안 예외 및 미처리 최상위 예외(500)를 일관된 형식으로 반환합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * MoplException 예외가 발생했을 때 적절한 HTTP 에러 응답을 반환합니다.
   *
   * @param ex 발생한 MoplException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(MoplException.class)
  public ResponseEntity<ErrorResponse> handleMoplException(MoplException ex) {
    ErrorCode errorCode = ex.getErrorCode();

    // 5xx 서버 에러 (사용자에게 내부 상세 정보를 숨김)
    if (errorCode.getHttpStatus().is5xxServerError()) {
      log.error(
          "[MoplException - Server Error] Code: {}, Message: {}, Details: {}",
          errorCode.getCode(),
          errorCode.getMessage(),
          ex.getDetails(),
          ex);

      ErrorResponse errorResponse =
          new ErrorResponse(
              errorCode.getCode(),
              errorCode.getMessage(),
              Map.of("message", "A system error occurred. Please contact the administrator."));
      return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    // 4xx 클라이언트 에러 (사용자에게 디버깅/원인 파악용 상세 정보 전달)
    log.warn(
        "[MoplException - Client Error] Code: {}, Message: {}, Details: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex.getDetails());

    ErrorResponse errorResponse =
        new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ex.getDetails());

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * DTO 등의 검증 및 바인딩이 실패했을 때 발생하는 예외를 처리합니다. 각 필드별 검증 실패 메시지를 details 맵에 담아 SYS01 코드와 함께 400 Bad
   * Request를 반환합니다. 타입 변환 에러가 있는 경우, 커스텀 컨버터 등에서 발생한 상세 예외 메시지(예: 지원하지 않는 정렬 방향)를 추출해 반환합니다. 보안을 위해
   * 원본 입력값(rejectedValue)은 응답과 로그에서 제외합니다.
   *
   * @param ex 발생한 BindException 또는 MethodArgumentNotValidException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
    SystemErrorCode errorCode = SystemErrorCode.INVALID_INPUT_VALUE;

    Map<String, Object> details = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      String field = fieldError.getField();
      String message = null;

      // 바인딩 에러(타입 변환 에러 등)인 경우 컨버터 등에서 발생시킨 가장 구체적인 원인 메시지 추출
      if (fieldError.isBindingFailure()) {
        try {
          PropertyAccessException pae = fieldError.unwrap(PropertyAccessException.class);
          message = pae.getMostSpecificCause().getMessage();
        } catch (Exception ignored) {
        }
      }

      // 바인딩 에러가 아닌 경우(@Valid 검증에 실패한 경우)
      if (message == null) {
        message = String.valueOf(fieldError.getDefaultMessage());
      }

      details.merge(
          field, message, (existing, incoming) -> String.valueOf(existing) + "; " + incoming);
    }

    log.warn(
        "[BindException] Code: {}, Message: {}, Details: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        details);

    ErrorResponse errorResponse =
        new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details);

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * JSON 요청 본문 해석에 실패했을 때 (잘못된 JSON 형식 또는 역직렬화 불가) 발생하는 예외를 처리합니다. SYS02 코드와 함께 400 Bad Request를
   * 반환합니다.
   *
   * @param ex 발생한 HttpMessageNotReadableException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    SystemErrorCode errorCode = SystemErrorCode.INVALID_JSON_FORMAT;

    log.warn(
        "[HttpMessageNotReadableException] Code: {}, Message: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            Map.of(
                "message", "Request body parsing failed (Malformed JSON or incorrect data type)"));

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * 지원하지 않는 HTTP 메서드(예: POST인 데서 GET 호출)를 호출했을 때 발생하는 예외를 처리합니다. SYS03 코드와 함께 405 Method Not
   * Allowed를 반환합니다.
   *
   * @param ex 발생한 HttpRequestMethodNotSupportedException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex) {
    SystemErrorCode errorCode = SystemErrorCode.METHOD_NOT_ALLOWED;

    log.warn(
        "[HttpRequestMethodNotSupportedException] Code: {}, Message: {}, Method: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex.getMethod());

    ErrorResponse errorResponse =
        new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            Map.of("supportedMethods", String.valueOf(ex.getSupportedHttpMethods())));

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * Spring Security의 권한 거부 예외(AccessDeniedException) 발생 시 이를 처리합니다. SYS04 코드와 함께 403 Forbidden을
   * 반환합니다.
   *
   * @param ex 발생한 AccessDeniedException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    SystemErrorCode errorCode = SystemErrorCode.ACCESS_DENIED;

    log.warn(
        "[AccessDeniedException] Code: {}, Message: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            errorCode.getCode(), errorCode.getMessage(), Map.of("message", ex.getMessage()));

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * 존재하지 않는 정적 리소스 또는 핸들러 경로 요청 시 발생하는 예외를 처리합니다. SYS05 에러 코드와 함께 404 Not Found를 반환하며, 불필요한 500 에러
   * 로그 적재를 방지합니다.
   *
   * @param ex 발생한 NoResourceFoundException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
    SystemErrorCode errorCode = SystemErrorCode.RESOURCE_NOT_FOUND;

    log.warn(
        "[NoResourceFoundException] Code: {}, Message: {}, ResourcePath: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex.getResourcePath());

    ErrorResponse errorResponse =
        new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            Map.of("resourcePath", String.valueOf(ex.getResourcePath())));

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * 자바 표준 SecurityException 예외 발생 시 이를 처리합니다. SYS04 코드와 함께 403 Forbidden을 반환합니다.
   *
   * @param ex 발생한 SecurityException 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
    SystemErrorCode errorCode = SystemErrorCode.ACCESS_DENIED;

    log.error(
        "[SecurityException] Code: {}, Message: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            errorCode.getCode(), errorCode.getMessage(), Map.of("message", ex.getMessage()));

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  /**
   * 다른 핸들러에서 잡지 않은 모든 최상위 예외(500 Internal Server Error)를 포괄하여 처리합니다. 에러가 외부로 날것 그대로 노출되어 보안 취약점이 되는
   * 것을 예방합니다.
   *
   * @param ex 발생한 Exception 인스턴스
   * @return 에러 메시지 데이터와 HTTP 상태 코드를 포함한 ResponseEntity
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllException(Exception ex) {
    SystemErrorCode errorCode = SystemErrorCode.INTERNAL_SERVER_ERROR;

    log.error(
        "[UnhandledException] Code: {}, Message: {}",
        errorCode.getCode(),
        errorCode.getMessage(),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            Map.of("message", "An unexpected system error occurred"));

    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }
}
