package com.sb10.mopl.common.realtime;

/*
 * STOMP 구독자에게 메시지를 전달하는 추상화입니다. 단일 인스턴스에서는 로컬 브로커로 바로 전달하고,
 * 다중 인스턴스(dev/prod)에서는 Redis Pub/Sub을 거쳐 모든 인스턴스에 팬아웃한 뒤 각자의 로컬 브로커로 전달합니다.
 */
public interface StompFanOutPublisher {

  void publish(String destination, Object payload);
}
