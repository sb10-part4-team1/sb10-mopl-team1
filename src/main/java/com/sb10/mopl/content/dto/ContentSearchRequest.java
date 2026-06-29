package com.sb10.mopl.content.dto;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.content.entity.ContentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ContentSearchRequest(
    ContentType typeEqual,
    String keywordLike,
    List<String> tagsIn,
    String cursor,
    UUID idAfter,
    @Min(value = 10, message = "페이지당 조회 개수는 최소 10개 이상이어야 합니다.")
        @Max(value = 50, message = "페이지당 조회 개수는 최대 50개 이하여야 합니다.")
        Integer limit,
    SortDirection sortDirection,
    @NotNull(message = "정렬 기준은 필수 항목입니다.") ContentSortBy sortBy) {
  public ContentSearchRequest {
    if (limit == null) {
      limit = 20;
    }
    if (sortDirection == null) {
      sortDirection = SortDirection.DESCENDING;
    }
  }
}
