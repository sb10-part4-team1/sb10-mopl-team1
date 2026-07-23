package com.sb10.mopl.conversation.controller;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.conversation.dto.DirectMessageDto;
import com.sb10.mopl.conversation.dto.DirectMessageSendRequest;
import com.sb10.mopl.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

// STOMP로 들어온 DM 전송 요청을 처리하고, 저장된 메시지를 같은 대화방 구독자에게 브로드캐스트합니다.
@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageSocketController {

  private final ConversationService conversationService;
  private final SimpMessagingTemplate messagingTemplate;

  // 메시지 전송(SEND /pub/conversations/{conversationId}/direct-messages)
  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void send(
      @DestinationVariable UUID conversationId,
      @Payload @Valid DirectMessageSendRequest request,
      Principal principal) {
    AuthenticatedUser sender = resolve(principal);

    DirectMessageDto dto =
        conversationService.sendDirectMessage(sender.id(), conversationId, request);

    // 메시지 수신(SUBSCRIBE /sub/conversations/{conversationId}/direct-messages)
    messagingTemplate.convertAndSend(
        "/sub/conversations/" + conversationId + "/direct-messages", dto);
  }

  // CONNECT 시점에 StompChannelInterceptor가 세션에 부여한 Principal에서 발신자를 꺼낸다.
  private AuthenticatedUser resolve(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser;
    }

    log.warn("[DM] 인증이 필요합니다.");

    throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
  }
}
