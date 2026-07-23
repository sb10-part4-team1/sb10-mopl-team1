package com.sb10.mopl.watchingsession.dto;

/**
 * 시청 참여 결과. 유저가 이미 다른 콘텐츠를 보고 있던 경우 세션이 이동하며, 이때 이전에 보고 있던 콘텐츠의 시청 세션 정보가
 * previousSession에 담긴다(없으면 null).
 */
public record WatchingSessionJoinResult(
    WatchingSessionDto session, WatchingSessionDto previousSession) {}
