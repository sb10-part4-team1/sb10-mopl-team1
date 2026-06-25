package com.sb10.mopl.common.pagination;

import java.util.UUID;

public record CursorPageRequest(
  String cursor,
  UUID idAfter,
  Integer limit,
  String sortBy,
  SortDirection sortDirection
) {
}
