package com.sb10.mopl.watchingsession.dto;

import com.sb10.mopl.content.dto.ContentSummary;
import com.sb10.mopl.user.dto.response.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto(
    UUID id, Instant createdAt, UserSummary watcher, ContentSummary content) {}
