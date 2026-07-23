package com.sb10.mopl.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ReviewDto(
    @Schema(description = "리뷰 ID") UUID id,
    @Schema(description = "콘텐츠 ID") UUID contentId,
    @Schema(description = "작성자 정보") ReviewAuthorDto author,
    @Schema(description = "리뷰 내용") String text,
    @Schema(description = "평점") Integer rating) {}
