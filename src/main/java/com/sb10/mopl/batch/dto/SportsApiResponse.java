package com.sb10.mopl.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportsApiResponse {

  private List<SportsContentDto> events;

  public static SportsApiResponse empty() {
    SportsApiResponse response = new SportsApiResponse();
    response.events = Collections.emptyList();
    return response;
  }

  public boolean isEmpty() {
    return events == null || events.isEmpty();
  }
}
