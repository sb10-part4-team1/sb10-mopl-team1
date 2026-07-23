package com.sb10.mopl.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record FollowDto(
    @Schema(description = "팔로우 ID") UUID id,
    @Schema(description = "팔로워 사용자 ID") UUID followerId,
    @Schema(description = "팔로우 대상 사용자 ID") UUID followeeId) {}
