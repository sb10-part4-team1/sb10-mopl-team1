package com.sb10.mopl.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewCreateRequest(
    @NotNull(message = "콘텐츠 ID는 필수 입니다.") UUID contentId,
    @NotBlank(message = "리뷰 내용은 필수 입니다.") String text,
    @NotNull(message = "리뷰 평점은 필수 입니다.")
        @Min(value = 1, message = "평점은 1점 이상이여야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
        Integer rating) {}
