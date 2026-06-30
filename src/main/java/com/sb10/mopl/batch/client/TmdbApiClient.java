package com.sb10.mopl.batch.client;

import com.sb10.mopl.batch.dto.TmdbApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class TmdbApiClient {

  private final RestClient restClient;

  public TmdbApiClient(@Qualifier("tmdbRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  // TODO PR 4: @CircuitBreaker, @Retry, @TimeLimiter 추가 예정
  // 인기 영화 목록
  public TmdbApiResponse fetchPopularMovies(int page) {
    return fetch("/movie/popular", page);
  }

  // 인기 TV 시리즈 목록
  public TmdbApiResponse fetchPopularTv(int page) {
    return fetch("/tv/popular", page);
  }

  private TmdbApiResponse fetch(String path, int page) {
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
        log.warn("TMDB 응답 null - path: {}, page: {}", path, page);
        return TmdbApiResponse.empty(); // retry로직 이후 예외 발행 예정
      }

      return response;

    } catch (RestClientException e) {
      log.error("TMDB API 호출 실패 - path: {}, page: {}, 원인: {}", path, page, e.getMessage());
      return TmdbApiResponse.empty(); // retry로직 이후 예외 발행 예정
    }
  }
}
