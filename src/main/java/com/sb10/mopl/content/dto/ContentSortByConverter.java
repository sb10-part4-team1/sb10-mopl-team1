package com.sb10.mopl.content.dto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ContentSortByConverter implements Converter<String, ContentSortBy> {

  @Override
  public ContentSortBy convert(String source) {
    if (source.isBlank()) {
      return null;
    }
    String trimmed = source.trim().toLowerCase();
    return switch (trimmed) {
      case "watchercount" -> ContentSortBy.POPULAR;
      case "createdat" -> ContentSortBy.CREATED_AT;
      case "rate" -> ContentSortBy.RATING;
      default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + source);
    };
  }
}
