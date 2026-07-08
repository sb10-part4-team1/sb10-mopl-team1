package com.sb10.mopl.content.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentType {
  MOVIE("movie"),
  TV_SERIES("tvSeries"),
  SPORT("sport");

  private final String value;

  ContentType(String value) {
    this.value = value;
  }

  /*
   * JSON 직렬화 및 역직렬화 시 프론트엔드 호환 포맷(camelCase)으로 값을 매핑해 줍니다.
   */
  @JsonValue
  public String getValue() {
    return value;
  }
}
