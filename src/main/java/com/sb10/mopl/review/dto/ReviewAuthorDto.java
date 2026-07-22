package com.sb10.mopl.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

// 리뷰 작성자를 표현하기 위한 DTO(응답구조를 맞추기위해 구현)
public record ReviewAuthorDto(
    @Schema(description = "사용자 ID") UUID userId,
    @Schema(description = "작성자 이름") String name,
    @Schema(description = "프로필 이미지 URL") String profileImageUrl) {}
