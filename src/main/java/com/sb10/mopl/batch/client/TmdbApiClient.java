package com.sb10.mopl.batch.client;

import static org.hibernate.query.results.Builders.fetch;

import com.sb10.mopl.batch.tmdb.dto.TmdbSearchResponse;
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
  public TmdbSearchResponse fetchPopularMovies(int page) {
    return fetch("/movie/popular", page);
  }

  // 인기 TV 시리즈 목록
  public TmdbSearchResponse fetchPopularTv(int page) {
    return fetch("/tv/popular", page);
  }

  private TmdbSearchResponse fetch(String path, int page) {
    try {
      TmdbSearchResponse response =
          restClient
              .get()
              .uri("{path}?language=ko-KR&page={page}", path, page)
              .retrieve()
              .body(TmdbSearchResponse.class);

      if (response == null) {
        log.warn("TMDB 응답 null - path: {}, page: {}", path, page);
        return TmdbSearchResponse.empty();
      }

      return response;

    } catch (RestClientException e) {
      log.error("TMDB API 호출 실패 - path: {}, page: {}, 원인: {}", path, page, e.getMessage());
      return TmdbSearchResponse.empty();
    }
  }
}
