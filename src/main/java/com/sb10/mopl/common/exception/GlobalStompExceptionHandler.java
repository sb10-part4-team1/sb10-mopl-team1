package com.sb10.mopl.common.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * STOMP @MessageMapping 컨트롤러 전역에서 발생하는 예외를 가로채서 처리합니다.
 *
 * <p>예외를 던진 세션의 사용자에게만 {@code /user/sub/queue/errors}로 응답합니다.
 */
@Slf4j
@ControllerAdvice
public class GlobalStompExceptionHandler {

  @MessageExceptionHandler(MoplException.class)
  @SendToUser("/sub/queue/errors")
  public ErrorResponse handleMoplException(MoplException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    log.warn("[STOMP] 처리 중 예외 발생: {}", errorCode.getCode(), ex);
    return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ex.getDetails());
  }

  // @Payload @Valid 검증 실패 시 발생
  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser("/sub/queue/errors")
  public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
    SystemErrorCode errorCode = SystemErrorCode.INVALID_INPUT_VALUE;

    Map<String, Object> details = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      details.merge(
          fieldError.getField(),
          String.valueOf(fieldError.getDefaultMessage()),
          (existing, incoming) -> existing + "; " + incoming);
    }

    log.warn("[STOMP] 검증 실패: {}", details);
    return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details);
  }

  // 위의 핸들러들이 처리하지 못한 나머지 예외들을 처리합니다.
  @MessageExceptionHandler(Exception.class)
  @SendToUser("/sub/queue/errors")
  public ErrorResponse handleException(Exception ex) {
    SystemErrorCode errorCode = SystemErrorCode.INTERNAL_SERVER_ERROR;
    log.error("[STOMP] 기타 예외 발생", ex);
    return new ErrorResponse(
        errorCode.getCode(),
        errorCode.getMessage(),
        Map.of("message", "An unexpected system error occurred"));
  }
}
