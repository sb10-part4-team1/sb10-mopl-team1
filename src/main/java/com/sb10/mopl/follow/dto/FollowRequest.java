package com.sb10.mopl.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowRequest(
    @Schema(description = "팔로우 대상 사용자 ID") @NotNull(message = "팔로우 대상자는 필수입니다.") UUID followeeId) {}
