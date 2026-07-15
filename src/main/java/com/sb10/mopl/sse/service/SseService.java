package com.sb10.mopl.sse.service;

import java.util.Collection;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * 실시간 알림(SSE)의 연결 및 전송 관리를 수행하는 공통 서비스 인터페이스입니다.
 */
public interface SseService {

  SseEmitter connect(UUID receiverId, UUID lastEventId);

  void send(Collection<UUID> receiverIds, String eventName, Object data);

  void broadcast(String eventName, Object data);
}
