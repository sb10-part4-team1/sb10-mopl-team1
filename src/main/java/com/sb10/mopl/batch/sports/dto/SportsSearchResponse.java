package com.sb10.mopl.batch.sports.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportsSearchResponse {

  private List<SportsContentDto> events;

  public static SportsSearchResponse empty() {
    SportsSearchResponse response = new SportsSearchResponse();
    response.events = Collections.emptyList();
    return response;
  }

  public boolean isEmpty() {
    return events == null || events.isEmpty();
  }
}
