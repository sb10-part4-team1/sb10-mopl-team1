package com.sb10.mopl.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * StompChannelInterceptor 등 @MessageMapping 컨트롤러에 도달하기 전(클라이언트 인바운드 채널 단계)에서 발생하는 예외를 처리합니다.
 *
 * <p>{@code @MessageExceptionHandler}는 컨트롤러 핸들러 실행 중 발생한 예외만 가로챌 수 있어, 채널 인터셉터 단계의 예외는 별도로 이
 * StompSubProtocolErrorHandler에서 처리해야 클라이언트가 동일한 형식(ErrorResponse)의 STOMP ERROR 프레임을 받을 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalStompChannelErrorHandler extends StompSubProtocolErrorHandler {

  private final ObjectMapper objectMapper;

  @Override
  public Message<byte[]> handleClientMessageProcessingError(
      Message<byte[]> clientMessage, Throwable ex) {
    MoplException moplException = findMoplException(ex);
    if (moplException == null) {
      return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    ErrorCode errorCode = moplException.getErrorCode();
    log.warn("[STOMP] 채널 인터셉터 단계에서 예외 발생: {}", errorCode.getCode(), moplException);

    ErrorResponse errorResponse =
        new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), moplException.getDetails());

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
    accessor.setMessage(errorCode.getMessage());
    accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
    accessor.setLeaveMutable(true);

    StompHeaderAccessor clientAccessor =
        MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
    if (clientAccessor != null && clientAccessor.getReceipt() != null) {
      accessor.setReceiptId(clientAccessor.getReceipt());
    }

    return handleInternal(accessor, writeValueAsBytes(errorResponse), ex, clientAccessor);
  }

  // preSend 단계의 예외는 MessageDeliveryException 등으로 감싸질 수 있어 원인 체인에서 MoplException을 탐색합니다.
  private MoplException findMoplException(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof MoplException moplException) {
        return moplException;
      }
      current = current.getCause();
    }
    return null;
  }

  private byte[] writeValueAsBytes(ErrorResponse errorResponse) {
    try {
      return objectMapper.writeValueAsBytes(errorResponse);
    } catch (JsonProcessingException e) {
      log.error("[STOMP] 에러 응답 직렬화에 실패했습니다.", e);
      return new byte[0];
    }
  }
}
