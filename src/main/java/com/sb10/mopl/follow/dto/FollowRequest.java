package com.sb10.mopl.follow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowRequest(@NotNull(message = "팔로우 대상자는 필수입니다.") UUID followeeId) {}
