package com.sb10.mopl.batch.client;

import com.sb10.mopl.batch.dto.SportsSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class SportsApiClient {

  private final RestClient restClient;

  public SportsApiClient(@Qualifier("sportsRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  // TODO PR 4: @CircuitBreaker, @Retry, @TimeLimiter 추가 예정
  public SportsSearchResponse fetchEventsByDay(String date, int leagueId) {
    try {
      SportsSearchResponse response =
          restClient
              .get()
              .uri("/eventsday.php?d={date}&l={leagueId}", date, leagueId)
              .retrieve()
              .body(SportsSearchResponse.class);

      if (response == null) {
        log.warn("SportsDB 응답 null - date: {}, leagueId: {}", date, leagueId);
        return SportsSearchResponse.empty();
      }

      return response;

    } catch (RestClientException e) {
      log.error(
          "SportsDB API 호출 실패 - date: {}, leagueId: {}, 원인: {}", date, leagueId, e.getMessage());
      return SportsSearchResponse.empty(); // TODO: 커스텀 메트릭 지표 추가
    }
  }
}
