package com.sb10.mopl.playlist.dto;

import java.util.List;
import java.util.UUID;

public record PlaylistContentSummaryDto(
    UUID id,
    String type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount) {}
