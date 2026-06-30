package com.sb10.mopl.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @NotBlank(message = "플레이리스트 제목은 필수입니다") @Size(max = 255, message = "플레이리스트 제목은 255자 이하여야합니다.")
        String title,
    @NotBlank(message = "플레이리스트 내용은 필수입니다.") String description) {}
