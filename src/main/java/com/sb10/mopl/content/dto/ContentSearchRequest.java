package com.sb10.mopl.content.dto;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.content.entity.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ContentSearchRequest(
    @Schema(description = "콘텐츠 타입") ContentType typeEqual,
    @Schema(description = "검색 키워드") String keywordLike,
    @Schema(description = "태그 목록") List<String> tagsIn,
    @Schema(description = "커서") String cursor,
    @Schema(description = "보조 커서") UUID idAfter,
    @Schema(description = "한 번에 가져올 개수", defaultValue = "20")
        @Min(value = 10, message = "페이지당 조회 개수는 최소 10개 이상이어야 합니다.")
        @Max(value = 50, message = "페이지당 조회 개수는 최대 50개 이하여야 합니다.")
        Integer limit,
    @Schema(description = "정렬 방향", defaultValue = "DESCENDING") SortDirection sortDirection,
    @Schema(description = "정렬 기준") @NotNull(message = "정렬 기준은 필수 항목입니다.") ContentSortBy sortBy) {
  public ContentSearchRequest {
    if (limit == null) {
      limit = 20;
    }
    if (sortDirection == null) {
      sortDirection = SortDirection.DESCENDING;
    }
  }
}
