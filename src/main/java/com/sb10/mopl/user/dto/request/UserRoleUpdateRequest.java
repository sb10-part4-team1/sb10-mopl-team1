package com.sb10.mopl.user.dto.request;

import com.sb10.mopl.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
    @Schema(description = "권한") @NotNull(message = "권한은 필수입니다.") UserRole role) {}
