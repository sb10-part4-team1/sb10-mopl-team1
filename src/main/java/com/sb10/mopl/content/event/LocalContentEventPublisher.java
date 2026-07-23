package com.sb10.mopl.content.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test", "default"})
@RequiredArgsConstructor
public class LocalContentEventPublisher implements ContentEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void publish(ContentEvent event) {
    log.info("[Spring 로컬 이벤트 발행] Content ID: {}, Type: {}", event.contentId(), event.eventType());
    eventPublisher.publishEvent(event);
  }
}
