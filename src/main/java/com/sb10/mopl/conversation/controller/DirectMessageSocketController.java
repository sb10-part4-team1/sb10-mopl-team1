package com.sb10.mopl.conversation.controller;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.conversation.dto.DirectMessageDto;
import com.sb10.mopl.conversation.dto.DirectMessageSendRequest;
import com.sb10.mopl.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageSocketController {

  private final ConversationService conversationService;
  private final SimpMessagingTemplate messagingTemplate;

  // SEND /pub/conversations/{conversationId}/direct-messages
  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void sendDirectMessage(
      @DestinationVariable UUID conversationId,
      @Payload @Valid DirectMessageSendRequest request,
      Principal principal) {
    AuthenticatedUser sender = resolveSender(principal);

    DirectMessageDto dto =
        conversationService.sendDirectMessage(sender.id(), conversationId, request);

    // SUBSCRIBE /sub/conversations/{conversationId}/direct-messages
    messagingTemplate.convertAndSend(
        "/sub/conversations/" + conversationId + "/direct-messages", dto);
  }

  private AuthenticatedUser resolveSender(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser;
    }
    throw new MoplException(
        AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", "인증이 필요합니다."));
  }
}
