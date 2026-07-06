package com.sb10.mopl.batch.client;

import com.sb10.mopl.batch.dto.SportsApiResponse;
import com.sb10.mopl.batch.exception.BatchErrorCode;
import com.sb10.mopl.batch.exception.BatchException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class SportsApiClient {

  private final RestClient restClient;

  public SportsApiClient(@Qualifier("sportsRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  /*
   * 지정된 날짜와 리그 ID의 경기 정보를 가져옵니다.
   * 429, 5xx, 네트워크 지연 발생 시 최대 2회 재시도를 수행합니다.
   */
  @Retryable(
      retryFor = {
        HttpServerErrorException.class, // 5xx
        HttpClientErrorException.TooManyRequests.class, // 429
        ResourceAccessException.class // network error, timeout
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2.0))
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
        log.error("SportsDB 응답 바디 누락(null) - date: {}, leagueId: {}", date, leagueId);
        throw new BatchException(
            BatchErrorCode.INVALID_API_RESPONSE,
            Map.of("message", "SportsDB API 응답 객체가 null입니다. 바디 유실 장애가 발생했습니다."));
      }

      return response;

    } catch (ResourceAccessException
        | HttpServerErrorException
        | HttpClientErrorException.TooManyRequests e) {
      log.warn(
          "SportsDB API 호출 장애 발생 - date: {}, leagueId: {}, 원인: {}", date, leagueId, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error(
          "SportsDB API 호출 중 예외 발생 (즉시 실패) - date: {}, leagueId: {}, 원인: {}",
          date,
          leagueId,
          e.getMessage());
      throw e;
    }
  }
}
