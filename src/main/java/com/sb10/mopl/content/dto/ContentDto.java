package com.sb10.mopl.content.dto;

import java.util.List;
import java.util.UUID;

public record ContentDto(
    UUID id,
    String type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount,
    long watcherCount) {}
