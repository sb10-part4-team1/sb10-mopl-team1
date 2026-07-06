package com.sb10.mopl.common.pagination;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SortDirectionConverter implements Converter<String, SortDirection> {

  @Override
  public SortDirection convert(String source) {
    if (source.isBlank()) {
      return null;
    }
    String trimmed = source.trim().toUpperCase();
    if ("ASC".equals(trimmed) || "ASCENDING".equals(trimmed)) {
      return SortDirection.ASCENDING;
    }
    if ("DESC".equals(trimmed) || "DESCENDING".equals(trimmed)) {
      return SortDirection.DESCENDING;
    }
    throw new IllegalArgumentException("지원하지 않는 정렬 방향입니다: " + source);
  }
}
