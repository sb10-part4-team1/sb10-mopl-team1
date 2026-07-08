package com.sb10.mopl.batch.job;

import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.listener.MoplBatchMetricsListener;
import com.sb10.mopl.batch.processor.TmdbItemProcessor;
import com.sb10.mopl.batch.reader.TmdbItemReader;
import com.sb10.mopl.batch.writer.ContentItemWriter;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/*
 * TMDB 인기 콘텐츠(영화 및 TV 시리즈)를 수집하기 위한 Spring Batch Job 설정 클래스입니다.
 *
 * 이 배치 작업은 다음과 같은 순서로 실행됩니다:
 *
 * 1. tmdbMovieStep: 인기 영화 데이터를 수집하여 DB에 커밋합니다. (완전 격리 실행)
 * 2. tmdbTvStep: 인기 TV 시리즈 데이터를 수집하여 DB에 커밋합니다. (영화 단계 완료 후 실행)
 */
@Configuration
@RequiredArgsConstructor
public class TmdbJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TmdbItemProcessor tmdbItemProcessor;
  private final ContentItemWriter contentItemWriter;
  private final TmdbItemReader tmdbItemReader;
  private final MoplBatchMetricsListener metricsListener; // 메트릭 리스너 추가

  /* TMDB 인기 콘텐츠 수집의 시작점이 되는 최상위 Job 객체를 정의합니다. 영화 수집 스텝을 완료한 뒤 순차적으로 TV 시리즈 수집 스텝을 실행합니다. */
  @Bean
  public Job tmdbJob() {
    return new JobBuilder("tmdbJob", jobRepository)
        .start(tmdbMovieStep())
        .next(tmdbTvStep())
        .listener(metricsListener) // Job 리스너 등록
        .build();
  }

  /* TMDB 영화 데이터를 읽고, 변환하고, 저장하는 단계를 구성합니다. */
  @Bean
  public Step tmdbMovieStep() {
    return new StepBuilder("tmdbMovieStep", jobRepository)
        .<TmdbContentDto, Content>chunk(20, transactionManager)
        .reader(tmdbItemReader)
        .processor(tmdbItemProcessor)
        .writer(contentItemWriter)
        .faultTolerant()
        .skipLimit(5)
        .skip(ContentException.class)
        .listener(metricsListener) // Step 리스너 등록
        .build();
  }

  /* TMDB TV 시리즈 데이터를 읽고, 변환하고, 저장하는 단계를 구성합니다. */
  @Bean
  public Step tmdbTvStep() {
    return new StepBuilder("tmdbTvStep", jobRepository)
        .<TmdbContentDto, Content>chunk(20, transactionManager)
        .reader(tmdbItemReader)
        .processor(tmdbItemProcessor)
        .writer(contentItemWriter)
        .faultTolerant()
        .skipLimit(5)
        .skip(ContentException.class)
        .listener(metricsListener) // Step 리스너 등록
        .build();
  }
}
