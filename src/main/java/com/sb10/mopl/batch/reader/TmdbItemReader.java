package com.sb10.mopl.batch.reader;

import com.sb10.mopl.batch.client.TmdbApiClient;
import com.sb10.mopl.batch.dto.TmdbApiResponse;
import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.content.entity.ContentType;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TMDB API로부터 데이터를 페이지 단위로 지연 로딩하여 반환하는 단일 공통 페이징 리더입니다. 실행 시점의 Step 이름(SpEL)을 분석하여 영화 또는 TV 시리즈
 * 수집인지를 자동으로 판별합니다.
 */
@Slf4j
@Component
@StepScope
public class TmdbItemReader implements ItemReader<TmdbContentDto> {

  private final TmdbApiClient tmdbApiClient;
  private final int maxPages;
  private final ContentType contentType;

  private List<TmdbContentDto> buffer = new ArrayList<>();
  private int index = 0;
  private int currentPage = 1;
  private int totalPages = 1;

  public TmdbItemReader(
      TmdbApiClient tmdbApiClient,
      @Value("${mopl.tmdb.batch.max-pages:1}") int maxPages,
      @Value("#{stepExecution.stepName}") String stepName) {
    this.tmdbApiClient = tmdbApiClient;
    this.maxPages = maxPages;
    // 실행 중인 Step의 이름에 "Movie"가 들어가 있으면 영화, 없으면 TV 시리즈로 간주
    this.contentType = stepName.contains("Movie") ? ContentType.MOVIE : ContentType.TV_SERIES;
  }

  @Override
  public TmdbContentDto read() {
    if (index >= buffer.size()) {
      if (currentPage > totalPages || currentPage > maxPages || currentPage > 500) {
        return null; // 수집 종료
      }

      fetchNextPage();

      if (buffer.isEmpty()) {
        return null;
      }
    }

    return buffer.get(index++);
  }

  private void fetchNextPage() {
    int page = currentPage++;
    String typeName = (contentType == ContentType.MOVIE) ? "영화" : "TV 시리즈";
    log.info("TMDB 인기 {} API 호출 - page: {}", typeName, page);

    TmdbApiResponse response =
        (contentType == ContentType.MOVIE)
            ? tmdbApiClient.fetchPopularMovies(page)
            : tmdbApiClient.fetchPopularTv(page);

    List<TmdbContentDto> results = response.getResults();
    // API 응답 구조 붕괴 상황에 대한 명확한 예외 처리
    if (results == null) {
      throw new IllegalStateException(
          "TMDB " + typeName + " API 응답 구조가 변경되었거나 비정상입니다. 'results' 필드가 null입니다.");
    }

    this.buffer = results;
    this.totalPages = response.getTotalPages();
    this.index = 0;
  }
}
