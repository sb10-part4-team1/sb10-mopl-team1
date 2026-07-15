package com.sb10.mopl.sse.kafka;

import com.sb10.mopl.sse.repository.SseEmitterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * 분산 서버에서 발행한 SSE 카프카 이벤트를 수신하여, 해당 서버에 물리적으로 연결된 Emitter에 전달하는 소비자 컴포넌트입니다.
 * 각 App 서버 인스턴스가 고유한 Group ID를 가지도록 동적으로 지정하여 브로드캐스팅(Fan-out) 효과를 구현합니다.
 */
@Slf4j
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class SseKafkaConsumer {

  private final SseEmitterRepository sseEmitterRepository;

  @KafkaListener(
      topics = "mopl-sse-topic",
      groupId = "mopl-sse-group-#{T(java.util.UUID).randomUUID().toString()}")
  public void consume(SseEventPayload payload) {
    log.info("SSE 카프카 이벤트 수신 - 이벤트: {}, 브로드캐스트 여부: {}", payload.eventName(), payload.isBroadcast());

    if (payload.isBroadcast()) {
      sseEmitterRepository.findAll().forEach(emitter -> sendToEmitter(emitter, payload));
    } else if (payload.receiverIds() != null) {
      sseEmitterRepository
          .findAllByReceiverIdsIn(payload.receiverIds())
          .forEach(emitter -> sendToEmitter(emitter, payload));
    }
  }

  private void sendToEmitter(SseEmitter emitter, SseEventPayload payload) {
    try {
      String eventId = payload.eventId() != null ? payload.eventId().toString() : "";
      emitter.send(SseEmitter.event().id(eventId).name(payload.eventName()).data(payload.data()));
    } catch (IOException e) {
      log.error("SseEmitter로 이벤트 전송 실패 (접속 끊김 감지)", e);
    }
  }
}
