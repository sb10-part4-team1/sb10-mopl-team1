package com.sb10.mopl.batch.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.cache.TmdbGenreCache;
import com.sb10.mopl.batch.client.TmdbApiClient;
import com.sb10.mopl.batch.dto.TmdbApiResponse;
import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.dto.TmdbGenreListDto;
import com.sb10.mopl.batch.dto.TmdbGenreListDto.TmdbGenreDto;
import com.sb10.mopl.batch.mapper.TmdbContentMapper;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = "mopl.tmdb.batch.max-pages=1")
@ActiveProfiles("test")
class BatchMetricsIntegrationTest {

  @Autowired private JobLauncher jobLauncher;
  @Autowired private Job tmdbJob;
  @Autowired private ContentRepository contentRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private TmdbGenreCache tmdbGenreCache;
  @Autowired private MeterRegistry meterRegistry;

  @MockitoBean private TmdbApiClient tmdbApiClient;
  @MockitoBean private TmdbContentMapper tmdbContentMapper;

  @BeforeEach
  void setUp() {
    // given: TMDB 장르 API 호출에 대비한 Mock 리턴 설정
    TmdbGenreListDto mockGenreList =
        new TmdbGenreListDto(List.of(new TmdbGenreDto(28, "액션"), new TmdbGenreDto(878, "SF")));
    when(tmdbApiClient.fetch("/genre/movie/list", null, TmdbGenreListDto.class))
        .thenReturn(mockGenreList);
    when(tmdbApiClient.fetch("/genre/tv/list", null, TmdbGenreListDto.class))
        .thenReturn(mockGenreList);

    contentRepository.deleteAllInBatch();
    tagRepository.deleteAllInBatch();
  }

  @AfterEach
  void tearDown() {
    contentRepository.deleteAllInBatch();
    tagRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("성공적으로 배치가 구동되면 프로메테우스용 커스텀 메트릭들이 올바르게 기록된다")
  void collectMetrics_success_whenJobRunsSuccessfully() throws Exception {
    // given: 매퍼 실제 코드 실행 및 인기 영화/TV 시리즈 API 모킹 응답 구성
    when(tmdbContentMapper.toEntity(any())).thenCallRealMethod();

    TmdbContentDto movie =
        new TmdbContentDto(10L, "테스트 영화", null, "영화 설명", "/poster.jpg", Set.of(28));
    TmdbApiResponse movieResponse = TmdbApiResponse.empty();
    ReflectionTestUtils.setField(movieResponse, "results", List.of(movie));
    ReflectionTestUtils.setField(movieResponse, "totalPages", 1);
    when(tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class)).thenReturn(movieResponse);

    TmdbContentDto tv = new TmdbContentDto(20L, null, "테스트 TV", "TV 설명", "/tv.jpg", Set.of(878));
    TmdbApiResponse tvResponse = TmdbApiResponse.empty();
    ReflectionTestUtils.setField(tvResponse, "results", List.of(tv));
    ReflectionTestUtils.setField(tvResponse, "totalPages", 1);
    when(tmdbApiClient.fetch("/tv/popular", 1, TmdbApiResponse.class)).thenReturn(tvResponse);

    JobParameters jobParameters =
        new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters();

    // when: TMDB 배치 잡(Job) 실행
    JobExecution jobExecution = jobLauncher.run(tmdbJob, jobParameters);

    // then: 잡 상태가 COMPLETED로 완료되었는지 검증
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // then: 1. mopl.batch.job.status 게이지(Gauge) 검증 (1.0 = COMPLETED)
    Double jobStatus =
        meterRegistry.find("mopl.batch.job.status").tag("jobName", "tmdbJob").gauge().value();
    assertThat(jobStatus).isEqualTo(1.0);

    // then: 2. mopl.batch.job.duration.seconds 타이머(Timer) 검증
    Timer durationTimer =
        meterRegistry.find("mopl.batch.job.duration.seconds").tag("jobName", "tmdbJob").timer();
    assertThat(durationTimer).isNotNull();
    assertThat(durationTimer.count()).isEqualTo(1L);

    // then: 3. mopl.batch.step.items.total 카운터(Counter) 검증 (스텝별 처리 건수)
    double movieReadCount =
        meterRegistry
            .find("mopl.batch.step.items.total")
            .tag("stepName", "tmdbMovieStep")
            .tag("type", "read")
            .counter()
            .count();
    assertThat(movieReadCount).isEqualTo(1.0);

    double movieWriteCount =
        meterRegistry
            .find("mopl.batch.step.items.total")
            .tag("stepName", "tmdbMovieStep")
            .tag("type", "write")
            .counter()
            .count();
    assertThat(movieWriteCount).isEqualTo(1.0);

    // then: 4. mopl.batch.collected.items.total 비즈니스 저장 카운터 검증
    double collectedNewCount =
        meterRegistry
            .find("mopl.batch.collected.items.total")
            .tag("contentType", "movie")
            .tag("status", "new_saved")
            .counter()
            .count();
    assertThat(collectedNewCount).isEqualTo(1.0);
  }
}
