package com.sb10.mopl.content.controller;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.content.dto.ContentChatDto;
import com.sb10.mopl.content.dto.ContentChatSendRequest;
import com.sb10.mopl.content.service.ContentChatService;
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

// STOMP로 들어온 콘텐츠 채팅 메시지를 같은 콘텐츠를 보는 구독자에게 브로드캐스트한다.
@Slf4j
@Controller
@RequiredArgsConstructor
public class ContentChatSocketController {

  private final ContentChatService contentChatService;
  private final SimpMessagingTemplate messagingTemplate;

  // 메시지 전송
  @MessageMapping("/contents/{contentId}/chat")
  public void send(
    @DestinationVariable UUID contentId,
    @Payload @Valid ContentChatSendRequest request,
    Principal principal) {
    AuthenticatedUser sender = resolve(principal);

    ContentChatDto dto = contentChatService.sendMessage(sender.id(), contentId, request);

    // 메시지 수신
    messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/chat", dto);
  }

  // CONNECT 시점에 StompChannelInterceptor가 세션에 부여한 Principal에서 발신자를 꺼낸다.
  private AuthenticatedUser resolve(Principal principal) {
    if (principal instanceof Authentication authentication
      && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser;
    }

    log.warn("[CHAT] 인증이 필요합니다.");

    throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
  }
}
