package com.sb10.mopl.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
  UUID id,
  UUID contentId,
  ReviewAuthorDto author,
  String text,
  Integer rating
) {
}
