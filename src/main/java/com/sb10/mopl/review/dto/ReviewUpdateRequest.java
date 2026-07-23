package com.sb10.mopl.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewUpdateRequest(
    @Schema(description = "리뷰 내용") @NotBlank(message = "리뷰 내용은 필수입니다.") String text,
    @Schema(description = "평점 (1~5)")
        @NotNull(message = "리뷰평점은 필수입니다.")
        @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
        Integer rating) {}
