package com.sb10.mopl.batch.client;

import com.sb10.mopl.batch.dto.SportsApiResponse;
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
  public SportsApiResponse fetchEventsByDay(String date, int leagueId) {
    try {
      SportsApiResponse response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/eventsday.php")
                          .queryParam("d", date)
                          .queryParam("l", leagueId)
                          .build())
              .retrieve()
              .body(SportsApiResponse.class);

      if (response == null) {
        log.warn("SportsDB 응답 null - date: {}, leagueId: {}", date, leagueId);
        return SportsApiResponse.empty(); // 그날 경기가 없는 것이며 잘못된 응답이 아님
      }

      return response;

    } catch (RestClientException e) {
      log.error(
          "SportsDB API 호출 실패 - date: {}, leagueId: {}, 원인: {}", date, leagueId, e.getMessage());
      return SportsApiResponse.empty(); // TODO: 커스텀 메트릭 지표 추가, retry로직 이후 예외 발행 예정
    }
  }
}
