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
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class SseKafkaProducer {

  private static final String SSE_TOPIC = "mopl-sse-topic";
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendNotification(
      UUID eventId, Collection<UUID> receiverIds, String eventName, Object data) {
    SseEventPayload payload = new SseEventPayload(eventId, receiverIds, eventName, data, false);
    log.info("SSE 카프카 이벤트 발행 - 이벤트: {}, ID: {}", eventName, eventId);
    kafkaTemplate
        .send(SSE_TOPIC, payload)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("SSE 카프카 이벤트 발행 실패 - ID: {}", eventId, ex);
              }
            });
  }

  public void broadcastNotification(UUID eventId, String eventName, Object data) {
    SseEventPayload payload = new SseEventPayload(eventId, null, eventName, data, true);
    log.info("SSE 카프카 브로드캐스트 이벤트 발행 - 이벤트: {}, ID: {}", eventName, eventId);
    kafkaTemplate
        .send(SSE_TOPIC, payload)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("SSE 카프카 브로드캐스트 이벤트 발행 실패 - ID: {}", eventId, ex);
              }
            });
  }
}
