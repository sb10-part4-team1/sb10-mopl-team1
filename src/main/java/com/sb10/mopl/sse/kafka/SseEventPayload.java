package com.sb10.mopl.sse.kafka;

import java.util.Collection;
import java.util.UUID;

/*
 * 카프카 토픽을 통해 분산 서버 간에 송수신될 SSE 알림 이벤트 페이로드 레코드입니다.
 */
public record SseEventPayload(
    Collection<UUID> receiverIds, String eventName, Object data, boolean isBroadcast) {}
