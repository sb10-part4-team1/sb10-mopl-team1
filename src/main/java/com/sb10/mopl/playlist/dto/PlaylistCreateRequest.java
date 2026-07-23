package com.sb10.mopl.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @Schema(description = "플레이리스트 제목")
        @NotBlank(message = "플레이리스트 제목은 필수입니다")
        @Size(max = 255, message = "플레이리스트 제목은 255자 이하여야합니다.")
        String title,
    @Schema(description = "플레이리스트 설명") @NotBlank(message = "플레이리스트 설명은 필수입니다.")
        String description) {}
