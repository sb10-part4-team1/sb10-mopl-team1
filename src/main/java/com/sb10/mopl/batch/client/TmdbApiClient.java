package com.sb10.mopl.batch.client;

import com.sb10.mopl.batch.dto.TmdbApiResponse;
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
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class TmdbApiClient {

  private final RestClient restClient;

  public TmdbApiClient(@Qualifier("tmdbRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  /*
   * TMDB 오픈 API로부터 인기 영화 또는 TV 시리즈 콘텐츠 목록을 가져옵니다.
   * 429, 500 계열, 네트워크 지연 발생 시 최대 3회(총 4회 시도) 지수 백오프 재시도를 수행합니다.
   */
  @Retryable(
      retryFor = {
        HttpServerErrorException.class, // 5xx
        HttpClientErrorException.TooManyRequests.class, // 429
        ResourceAccessException.class // network error, timeout
      },
      maxAttempts = 4,
      backoff = @Backoff(delay = 2000, multiplier = 2.0))
  public TmdbApiResponse fetch(String path, int page) {
    try {
      TmdbApiResponse response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(path)
                          .queryParam("language", "ko-KR")
                          .queryParam("page", page)
                          .build())
              .retrieve()
              .body(TmdbApiResponse.class);

      if (response == null) {
        log.error("TMDB 응답 바디 누락(null) - path: {}, page: {}", path, page);
        throw new BatchException(
            BatchErrorCode.INVALID_API_RESPONSE,
            Map.of("message", "TMDB API 응답 객체가 null입니다. 바디 유실 장애가 발생했습니다."));
      }

      return response;

    } catch (RestClientException e) {
      log.error("TMDB API 호출 에러 (재시도 대상) - path: {}, page: {}, 원인: {}", path, page, e.getMessage());
      throw e;
    }
  }
}
