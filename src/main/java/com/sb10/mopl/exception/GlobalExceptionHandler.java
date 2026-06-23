package com.sb10.mopl.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 전역에서 발생하는 예외를 가로채서 처리하는 컨트롤러 어드바이스 클래스입니다. MoplException을 비롯하여 프레임워크 수준의 바인딩 에러, JSON 형식
 * 에러, 지원하지 않는 HTTP 메서드 호출, 보안 예외 및 미처리 최상위 예외(500)를 일관된 형식으로 반환합니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
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
   * DTO 등의 @Valid 검증이 실패했을 때 발생하는 예외를 처리합니다. 각 필드별 검증 실패 메시지를 details 맵에 담아 SYS01 코드와 함께 400 Bad
   * Request를 반환합니다.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    SystemErrorCode errorCode = SystemErrorCode.INVALID_INPUT_VALUE;

    Map<String, Object> details = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      details.put(
          fieldError.getField(),
          Map.of(
              "rejectedValue", String.valueOf(fieldError.getRejectedValue()),
              "message", String.valueOf(fieldError.getDefaultMessage())));
    }

    log.warn(
        "[ValidationException] Code: {}, Message: {}, Details: {}",
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
   * 자바 표준 SecurityException 예외 발생 시 이를 처리합니다. SYS04 코드와 함께 403 Forbidden을 반환합니다.
   *
   * <p>*참고: Spring Security 활성화 시 org.springframework.security.access.AccessDeniedException도 이 곳에
   * 핸들러를 추가하거나, SecurityConfig의 AccessDeniedHandler와 연결하여 전역 통일할 수 있습니다.
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
