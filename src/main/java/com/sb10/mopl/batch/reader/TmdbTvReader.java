package com.sb10.mopl.batch.reader;

import com.sb10.mopl.batch.client.TmdbApiClient;
import com.sb10.mopl.batch.dto.TmdbApiResponse;
import com.sb10.mopl.batch.dto.TmdbContentDto;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** TMDB 인기 TV 시리즈(TV) 데이터를 페이지 단위로 지연 로딩(Lazy Loading)하여 반환하는 페이징 리더입니다. */
@Slf4j
@Component
@StepScope
public class TmdbTvReader implements ItemReader<TmdbContentDto> {

  private final TmdbApiClient tmdbApiClient;

  private List<TmdbContentDto> buffer = new ArrayList<>();
  private int index = 0;
  private int currentPage = 1;
  private int totalPages = 1;

  @Value("${mopl.tmdb.batch.max-pages:1}")
  private int maxPages;

  public TmdbTvReader(TmdbApiClient tmdbApiClient) {
    this.tmdbApiClient = tmdbApiClient;
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
    log.info("TMDB 인기 TV 시리즈 API 호출 - page: {}", page);

    TmdbApiResponse response = tmdbApiClient.fetchPopularTv(page);

    this.buffer = response.getResults();
    this.totalPages = response.getTotalPages();
    this.index = 0;
  }
}
