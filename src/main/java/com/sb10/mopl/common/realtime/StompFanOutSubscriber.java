package com.sb10.mopl.common.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/*
 * Redis 채널로 들어온 팬아웃 메시지를 이 인스턴스에 붙어있는 STOMP 세션들에게 전달합니다.
 * (발행 인스턴스 자신도 이 채널을 구독하므로, 발행자의 로컬 구독자에게도 이 경로로만 전달됩니다.)
 */
@Slf4j
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class StompFanOutSubscriber implements MessageListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      StompFanOutMessage fanOutMessage =
          objectMapper.readValue(message.getBody(), StompFanOutMessage.class);
      messagingTemplate.convertAndSend(fanOutMessage.destination(), fanOutMessage.payload());
    } catch (Exception e) {
      log.error("STOMP fan-out 메시지 처리에 실패했습니다.", e);
    }
  }
}
