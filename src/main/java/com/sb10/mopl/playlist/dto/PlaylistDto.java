package com.sb10.mopl.playlist.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
    UUID id,
    PlaylistOwnerDto owner,
    String title,
    String description,
    Instant updatedAt,
    long subscriberCount,
    boolean subscribedByMe,
    List<PlaylistContentSummaryDto> contents) {}
