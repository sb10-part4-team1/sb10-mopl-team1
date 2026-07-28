package com.sb10.mopl.common.realtime;

import com.fasterxml.jackson.databind.JsonNode;

/*
 * Redis 채널로 주고받는 팬아웃 메시지 봉투. payload는 원래 DTO 타입 정보 없이 JsonNode로 담아,
 * 발행자(watch/chat/DM)와 구독자가 서로 구체 타입을 알 필요가 없도록 합니다.
 */
record StompFanOutMessage(String destination, JsonNode payload) {}
