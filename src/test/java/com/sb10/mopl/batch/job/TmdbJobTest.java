package com.sb10.mopl.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.client.TmdbApiClient;
import com.sb10.mopl.batch.dto.TmdbApiResponse;
import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.mapper.TmdbContentMapper;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "mopl.tmdb.batch.max-pages=2")
@ActiveProfiles("test")
class TmdbJobTest {

  @Autowired private JobLauncher jobLauncher;

  @Autowired private Job tmdbJob;

  @Autowired private ContentRepository contentRepository;

  @MockitoBean private TmdbApiClient tmdbApiClient;

  @MockitoBean private TmdbContentMapper tmdbContentMapper;

  @BeforeEach
  void setUp() {
    contentRepository.deleteAllInBatch();
  }

  @AfterEach
  void tearDown() {
    contentRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("다중 페이지 영화 및 TV 데이터가 필터링/기본값 적용과 함께 순차적 Step으로 모두 수집된다")
  void tmdbJob_collectDataWithFilteringAndDefaultValues_whenTitlesAreMissingOrImagesAreEmpty()
      throws Exception {
    // given: 영화 1페이지 DTO 구성
    TmdbContentDto normalMovie =
        new TmdbContentDto(1L, "정상 영화", null, "정상 설명", "/path.jpg", List.of(1));
    TmdbContentDto missingTitleMovie =
        new TmdbContentDto(2L, null, null, "설명은 있음", "/path.jpg", List.of(1));
    TmdbContentDto noPosterMovie =
        new TmdbContentDto(3L, "포스터 없는 영화", null, "설명", null, List.of(1));

    // 영화 1페이지 API 응답 모킹 (totalPages = 2 설정)
    TmdbApiResponse movieResponsePage1 = TmdbApiResponse.empty();
    org.springframework.test.util.ReflectionTestUtils.setField(
        movieResponsePage1, "results", List.of(normalMovie, missingTitleMovie, noPosterMovie));
    org.springframework.test.util.ReflectionTestUtils.setField(movieResponsePage1, "totalPages", 2);
    when(tmdbApiClient.fetchPopularMovies(1)).thenReturn(movieResponsePage1);

    // 영화 2페이지 DTO 구성 (다중 페이지 순회 검증용)
    TmdbContentDto noOverviewMovie =
        new TmdbContentDto(4L, "설명 없는 영화", null, null, "/path.jpg", List.of(1));
    TmdbApiResponse movieResponsePage2 = TmdbApiResponse.empty();
    org.springframework.test.util.ReflectionTestUtils.setField(
        movieResponsePage2, "results", List.of(noOverviewMovie));
    org.springframework.test.util.ReflectionTestUtils.setField(movieResponsePage2, "totalPages", 2);
    when(tmdbApiClient.fetchPopularMovies(2)).thenReturn(movieResponsePage2);

    // TV 시리즈 1페이지 DTO 구성 (TV Step 실행 검증용 - name 필드 사용으로 TV 시리즈 구분)
    TmdbContentDto normalTv =
        new TmdbContentDto(5L, null, "정상 TV시리즈", "TV 설명", "/tv.jpg", List.of(1));
    TmdbApiResponse tvResponsePage1 = TmdbApiResponse.empty();
    org.springframework.test.util.ReflectionTestUtils.setField(
        tvResponsePage1, "results", List.of(normalTv));
    org.springframework.test.util.ReflectionTestUtils.setField(tvResponsePage1, "totalPages", 1);
    when(tmdbApiClient.fetchPopularTv(1)).thenReturn(tvResponsePage1);
    when(tmdbApiClient.fetchPopularTv(2)).thenReturn(TmdbApiResponse.empty());

    // 매퍼는 실제 자바 코드 동작을 수행하도록 모킹
    when(tmdbContentMapper.toEntity(any())).thenCallRealMethod();

    // when
    JobExecution jobExecution =
        jobLauncher.run(
            tmdbJob,
            new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());

    // then: 전체 잡 성공 확인
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // 영화 및 TV 수집 단계 스텝 이름이 모두 실행 이력에 존재하는지 확인
    assertThat(jobExecution.getStepExecutions())
        .extracting("stepName")
        .containsExactlyInAnyOrder("tmdbMovieStep", "tmdbTvStep");

    List<Content> saved = contentRepository.findAll();
    // 제목 없는 영화(missingTitleMovie)는 필터링되어 제외되므로 [영화 3건 + TV 1건] = 총 4건 저장
    assertThat(saved).hasSize(4);

    // 썸네일 누락 시 기본 이미지 경로로 설정되었는지 확인
    Content noPosterSaved =
        saved.stream().filter(c -> c.getTitle().equals("포스터 없는 영화")).findFirst().orElseThrow();
    assertThat(noPosterSaved.getThumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");

    // 설명 누락 시 '설명이 없는 컨텐츠입니다.'으로 설정되었는지 확인
    Content noOverviewSaved =
        saved.stream().filter(c -> c.getTitle().equals("설명 없는 영화")).findFirst().orElseThrow();
    assertThat(noOverviewSaved.getDescription()).isEqualTo("설명이 없는 컨텐츠입니다.");

    // TV 시리즈 스텝이 정상적으로 가공 저장했는지 확인
    Content tvSaved =
        saved.stream().filter(c -> c.getTitle().equals("정상 TV시리즈")).findFirst().orElseThrow();
    assertThat(tvSaved.getType()).isEqualTo(ContentType.TV_SERIES);
  }

  @Test
  @DisplayName("영화 스텝이 6번째 에러로 실패하면 TV 스텝은 아예 실행되지 않고 중단된다")
  void tmdbJob_failJobAndSkipTvStep_whenMovieStepExceedsSkipLimit() throws Exception {
    // given: 에러를 유발할 DTO 6개 구성
    List<TmdbContentDto> errorMovies =
        List.of(
            new TmdbContentDto(1L, "에러 영화1", null, "설명", "/p1.jpg", List.of(1)),
            new TmdbContentDto(2L, "에러 영화2", null, "설명", "/p2.jpg", List.of(1)),
            new TmdbContentDto(3L, "에러 영화3", null, "설명", "/p3.jpg", List.of(1)),
            new TmdbContentDto(4L, "에러 영화4", null, "설명", "/p4.jpg", List.of(1)),
            new TmdbContentDto(5L, "에러 영화5", null, "설명", "/p5.jpg", List.of(1)),
            new TmdbContentDto(6L, "에러 영화6", null, "설명", "/p6.jpg", List.of(1)));

    TmdbApiResponse mockMovieResponse = TmdbApiResponse.empty();
    org.springframework.test.util.ReflectionTestUtils.setField(
        mockMovieResponse, "results", errorMovies);
    org.springframework.test.util.ReflectionTestUtils.setField(mockMovieResponse, "totalPages", 1);

    when(tmdbApiClient.fetchPopularMovies(1)).thenReturn(mockMovieResponse);

    // 매퍼에서 RuntimeException을 던지도록 모킹하여 에러 발생 시뮬레이션
    when(tmdbContentMapper.toEntity(any())).thenThrow(new RuntimeException("테스트용 변환 실패 예외"));

    // when
    JobExecution jobExecution =
        jobLauncher.run(
            tmdbJob,
            new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());

    // then: 스킵 제한이 5이므로 6번째 에러 발생 시 잡이 실패(FAILED) 상태가 됨
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

    // 영화 스텝은 돌았지만 TV 스텝은 실행 목록에 들어가면 안 됨
    assertThat(jobExecution.getStepExecutions())
        .extracting("stepName")
        .containsExactly("tmdbMovieStep");

    // TV API는 전혀 호출되지 않았음을 보장
    verify(tmdbApiClient, never()).fetchPopularTv(anyInt());
  }
}
