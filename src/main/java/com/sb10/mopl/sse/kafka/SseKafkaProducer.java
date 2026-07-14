package com.sb10.mopl.sse.kafka;

import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 * SSE 알림 메시지 이벤트를 카프카의 전역 토픽으로 발행(Publish)하는 프로듀서 컴포넌트입니다.
 */
@Slf4j
@Component
@Profile({"prod", "aws", "dev"})
@RequiredArgsConstructor
public class SseKafkaProducer {

  private static final String SSE_TOPIC = "mopl-sse-topic";
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendNotification(Collection<UUID> receiverIds, String eventName, Object data) {
    SseEventPayload payload = new SseEventPayload(receiverIds, eventName, data, false);
    log.info("SSE 카프카 이벤트 발행 - 수신자: {}, 이벤트: {}", receiverIds, eventName);
    kafkaTemplate.send(SSE_TOPIC, payload);
  }

  public void broadcastNotification(String eventName, Object data) {
    SseEventPayload payload = new SseEventPayload(null, eventName, data, true);
    log.info("SSE 카프카 브로드캐스트 이벤트 발행 - 이벤트: {}", eventName);
    kafkaTemplate.send(SSE_TOPIC, payload);
  }
}
