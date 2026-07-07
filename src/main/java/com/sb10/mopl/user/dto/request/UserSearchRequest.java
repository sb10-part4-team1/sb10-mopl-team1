package com.sb10.mopl.user.dto.request;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.user.entity.UserRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UserSearchRequest(
    String emailLike,
    UserRole roleEqual,
    Boolean isLocked,
    String cursor,
    UUID idAfter,
    @NotNull(message = "limit은 필수입니다.")
        @Positive(message = "limit은 1 이상이어야 합니다.")
        @Max(value = 100, message = "limit은 100 이하여야 합니다.")
        Integer limit,
    @NotNull(message = "정렬 방향은 필수입니다.") SortDirection sortDirection,
    @NotNull(message = "정렬 기준은 필수입니다.") SortBy sortBy) {

  public enum SortBy {
    name,
    email,
    createdAt,
    isLocked,
    role
  }
}
