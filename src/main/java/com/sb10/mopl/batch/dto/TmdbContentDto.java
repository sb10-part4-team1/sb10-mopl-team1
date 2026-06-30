package com.sb10.mopl.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbContentDto(
    Long id, // 식별자로 사용할 수도 있음
    String title, // Movie 전용 제목
    String name, // TV 전용 제목
    String overview, // Description
    @JsonProperty("poster_path") String posterPath, // ThumbnailUrl
    @JsonProperty("genre_ids") List<Integer> genreIds) { // Tag

  /** Movie: title 필드 사용 TV: name 필드 사용 */
  public String resolveTitle() {
    if (title != null && !title.isBlank()) {
      return title;
    }
    if (name != null && !name.isBlank()) {
      return name;
    }
    return null;
  }

  /** title 필드 존재 여부로 Movie/TV 구분 */
  public boolean isMovie() {
    return title != null && !title.isBlank();
  }
}
