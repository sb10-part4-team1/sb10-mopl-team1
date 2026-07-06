package com.sb10.mopl.batch.client;

import com.sb10.mopl.batch.exception.BatchErrorCode;
import com.sb10.mopl.batch.exception.BatchException;
import java.util.Map;
import java.util.Optional;
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
public class TmdbApiClient {

  private final RestClient restClient;

  public TmdbApiClient(@Qualifier("tmdbRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  /*
   * TMDB 오픈 API로부터 데이터를 가져옵니다.
   * page 파라미터가 null일 경우 쿼리 파라미터에서 제외됩니다. (예: 장르 목록 수집 시)
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
  public <T> T fetch(String path, Integer page, Class<T> responseType) {
    try {
      T response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(path)
                          .queryParam("language", "ko-KR")
                          .queryParamIfPresent("page", Optional.ofNullable(page))
                          .build())
              .retrieve()
              .body(responseType);

      if (response == null) {
        log.error("TMDB 응답 바디 누락(null) - path: {}, page: {}", path, page);
        throw new BatchException(
            BatchErrorCode.INVALID_API_RESPONSE,
            Map.of("message", "TMDB API 응답 객체가 null입니다. 바디 유실 장애가 발생했습니다."));
      }

      return response;

    } catch (ResourceAccessException
        | HttpServerErrorException
        | HttpClientErrorException.TooManyRequests e) {
      log.warn("TMDB API 호출 장애 발생 - path: {}, page: {}, 원인: {}", path, page, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error(
          "TMDB API 호출 중 예외 발생 (즉시 실패) - path: {}, page: {}, 원인: {}", path, page, e.getMessage());
      throw e;
    }
  }
}
