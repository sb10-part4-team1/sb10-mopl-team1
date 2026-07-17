package com.sb10.mopl.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * STOMP @MessageMapping 컨트롤러 전역에서 발생하는 MoplException을 가로채서 처리합니다.
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
}
