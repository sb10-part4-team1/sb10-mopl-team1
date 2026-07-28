package com.sb10.mopl.common.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

/*
 * 로컬 브로커로 바로 보내지 않고 Redis 채널에 발행합니다. 같은 채널을 구독 중인 모든 인스턴스의
 * StompFanOutSubscriber가 이를 받아 각자의 로컬 브로커(SimpMessagingTemplate)로 전달하므로,
 * 발행 인스턴스와 구독자가 연결된 인스턴스가 달라도 메시지가 전달됩니다.
 */
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class RedisStompFanOutPublisher implements StompFanOutPublisher {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final ChannelTopic stompFanOutTopic;

  @Override
  public void publish(String destination, Object payload) {
    try {
      String json =
          objectMapper.writeValueAsString(
              new StompFanOutMessage(destination, objectMapper.valueToTree(payload)));
      redisTemplate.convertAndSend(stompFanOutTopic.getTopic(), json);
    } catch (Exception e) {
      throw new IllegalStateException(
          "STOMP fan-out 메시지 발행에 실패했습니다: destination=" + destination, e);
    }
  }
}
