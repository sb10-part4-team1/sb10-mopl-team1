package com.sb10.mopl.user.dto;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UserSearchRequest(
    @Schema(description = "이메일") String emailLike,
    @Schema(description = "권한") UserRole roleEqual,
    @Schema(description = "계정 잠금 상태") Boolean isLocked,
    @Schema(description = "커서") String cursor,
    @Schema(description = "보조 커서") UUID idAfter,
    @Schema(description = "한 번에 가져올 개수")
        @NotNull(message = "limit은 필수입니다.")
        @Positive(message = "limit은 1 이상이어야 합니다.")
        @Max(value = 100, message = "limit은 100 이하여야 합니다.")
        Integer limit,
    @Schema(description = "정렬 방향") @NotNull(message = "정렬 방향은 필수입니다.") SortDirection sortDirection,
    @Schema(description = "정렬 기준") @NotNull(message = "정렬 기준은 필수입니다.") SortBy sortBy) {

  public enum SortBy {
    name,
    email,
    createdAt,
    isLocked,
    role
  }
}
