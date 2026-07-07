package com.sb10.mopl.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenreListDto(List<TmdbGenreDto> genres) { //

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TmdbGenreDto(Integer id, String name) {}
}
