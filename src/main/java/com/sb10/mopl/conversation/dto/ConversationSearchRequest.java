package com.sb10.mopl.conversation.dto;

import com.sb10.mopl.common.pagination.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationSearchRequest(
  String keywordLike,
  String cursor,
  UUID idAfter,
  @Min(value = 10, message = "페이지당 조회 개수는 10개 이상이어야 합니다.")
  @Max(value = 50, message = "페이지당 조회 개수는 50개 이하여야 합니다.")
  Integer limit,
  SortDirection sortDirection,
  @NotNull(message = "정렬 기준은 필수 항목입니다.") SortBy sortBy) {

  public ConversationSearchRequest {
    if (limit == null) {
      limit = 20;
    }
    if (sortDirection == null) {
      sortDirection = SortDirection.DESCENDING;
    }
  }

  public enum SortBy {
    createdAt
  }
}
