package com.sb10.mopl.review.dto;

import java.util.UUID;

public record ReviewAuthorDto(
  UUID userId,
  String name,
  String profileImageUrl
) {

}
