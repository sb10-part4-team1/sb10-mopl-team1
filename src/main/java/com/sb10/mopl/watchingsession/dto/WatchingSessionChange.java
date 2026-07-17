package com.sb10.mopl.watchingsession.dto;

public record WatchingSessionChange(
    ChangeType type, WatchingSessionDto watchingSession, long watcherCount) {}
