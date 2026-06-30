package com.sb10.mopl.batch.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbSearchResponse {

  private int page;
  private List<TmdbContentDto> results;

  @JsonProperty("total_pages")
  private int totalPages;

  @JsonProperty("total_results")
  private int totalResults;

  // fallback용 빈 응답 생성
  public static TmdbSearchResponse empty() {
    TmdbSearchResponse response = new TmdbSearchResponse();
    response.results = Collections.emptyList();
    response.totalPages = 0;
    response.totalResults = 0;
    return response;
  }

  public boolean isEmpty() {
    return results == null || results.isEmpty();
  }
}
