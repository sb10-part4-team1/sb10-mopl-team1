package com.sb10.mopl.content.dto;

import com.sb10.mopl.content.entity.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContentCreateRequest(
    @Schema(description = "콘텐츠 타입") @NotNull(message = "콘텐츠 타입은 필수 항목입니다.") ContentType type,
    @Schema(description = "콘텐츠 제목")
        @NotBlank(message = "제목은 필수 항목입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,
    @Schema(description = "콘텐츠 설명") @NotBlank(message = "설명은 필수 항목입니다.") String description,
    @Schema(description = "콘텐츠 태그 목록") List<String> tags) {}
