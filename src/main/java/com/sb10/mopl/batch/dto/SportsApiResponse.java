package com.sb10.mopl.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportsApiResponse {

  private List<SportsContentDto> events;
  private boolean hasEventsKey = false;

  public static SportsApiResponse empty() {
    SportsApiResponse response = new SportsApiResponse();
    response.events = Collections.emptyList();
    response.hasEventsKey = true; // 수동 생성된 fallback이므로 포맷 붕괴로 처리되지 않도록 설정
    return response;
  }

  /**
   * The Sports DB의 API응답이 변조되었는지 확인하기 위한 설정입니다.
   *
   * <p>events 필드가 존재하지 않으면 hasEventsKey가 기본값인 false가 됩니다.
   */
  @JsonSetter("events")
  public void setEvents(List<SportsContentDto> events) {
    this.events = events;
    this.hasEventsKey = true;
  }

  public boolean isEmpty() {
    return events == null || events.isEmpty();
  }
}
