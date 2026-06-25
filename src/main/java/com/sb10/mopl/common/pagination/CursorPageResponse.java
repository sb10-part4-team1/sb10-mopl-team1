package com.sb10.mopl.common.pagination;

import java.util.List;
import java.util.UUID;

public record CursorPageResponse<T>(
    List<T> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection) {}
