package com.sb10.mopl.content.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class KafkaContentEventPublisher implements ContentEventPublisher {

  private static final String TOPIC_NAME = "content-sync-events";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  public void publish(ContentEvent event) {
    log.info(
        "[Kafka 분산 이벤트 발행] Topic: {}, Content ID: {}, Type: {}",
        TOPIC_NAME,
        event.contentId(),
        event.eventType());
    kafkaTemplate.send(TOPIC_NAME, event.contentId().toString(), event);
  }
}
