package com.sb10.mopl.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PlaylistOwnerDto(
    @Schema(description = "사용자 ID") UUID userId,
    @Schema(description = "사용자 이름") String name,
    @Schema(description = "프로필 이미지 URL") String profileImageUrl) {}
