package com.sb10.mopl.content.dto;

import com.sb10.mopl.content.entity.ContentType;
import java.util.List;
import java.util.UUID;

public record ContentSummary(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount) {}
