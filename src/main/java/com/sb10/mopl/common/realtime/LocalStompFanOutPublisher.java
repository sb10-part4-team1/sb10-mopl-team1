package com.sb10.mopl.common.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/*
 * 단일 인스턴스(local/test) 전용 구현체입니다. 이 두 프로필은 Redis 자체를 쓰지 않으므로
 * (application-local.yaml, application-test.yaml의 RedisAutoConfiguration 제외 참고),
 * Redis를 거치지 않고 로컬 브로커로 바로 전달합니다.
 */
@Component
@Profile("!prod & !dev")
@RequiredArgsConstructor
public class LocalStompFanOutPublisher implements StompFanOutPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public void publish(String destination, Object payload) {
    messagingTemplate.convertAndSend(destination, payload);
  }
}
