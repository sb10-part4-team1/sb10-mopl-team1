package com.sb10.mopl.batch.job;

import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.processor.TmdbItemProcessor;
import com.sb10.mopl.batch.reader.TmdbMovieReader;
import com.sb10.mopl.batch.reader.TmdbTvReader;
import com.sb10.mopl.batch.writer.TmdbItemWriter;
import com.sb10.mopl.content.entity.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * TMDB 인기 콘텐츠(영화 및 TV 시리즈)를 수집하기 위한 Spring Batch Job 설정 클래스입니다.
 *
 * <p>이 배치 작업은 다음과 같은 순서로 실행됩니다: 1. tmdbMovieStep: 인기 영화 데이터를 수집하여 DB에 커밋합니다. (완전 격리 실행) 2.
 * tmdbTvStep: 인기 TV 시리즈 데이터를 수집하여 DB에 커밋합니다. (영화 단계 완료 후 실행)
 */
@Configuration
@RequiredArgsConstructor
public class TmdbJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TmdbMovieReader tmdbMovieReader;
  private final TmdbTvReader tmdbTvReader;
  private final TmdbItemProcessor tmdbItemProcessor;
  private final TmdbItemWriter tmdbItemWriter;

  /** TMDB 인기 콘텐츠 수집의 시작점이 되는 최상위 Job 객체를 정의합니다. 영화 수집 스텝을 완료한 뒤 순차적으로 TV 시리즈 수집 스텝을 실행합니다. */
  @Bean
  public Job tmdbJob() {
    return new JobBuilder("tmdbJob", jobRepository)
        .start(tmdbMovieStep())
        .next(tmdbTvStep())
        .build();
  }

  /** TMDB 영화 데이터를 읽고, 변환하고, 저장하는 단계를 구성합니다. 메서드 체이닝 순서대로 다음과 같이 동작합니다. */
  @Bean
  public Step tmdbMovieStep() {
    return new StepBuilder("tmdbMovieStep", jobRepository)
        // 1. 한번에 트랜잭션을 걸어 처리할 단위(20건)를 지정합니다.
        .<TmdbContentDto, Content>chunk(20, transactionManager)
        // 2. TMDB API로부터 인기 영화 데이터를 페이지 단위로 지연 로딩 공급합니다.
        .reader(tmdbMovieReader)
        // 3. 공급받은 DTO의 데이터를 검증하고 엔티티로 변환합니다.
        .processor(tmdbItemProcessor)
        // 4. 변환된 20건을 모아 중복을 제거한 뒤 DB에 한꺼번에 저장합니다.
        .writer(tmdbItemWriter)
        // 5. 예외 발생 시 배치가 즉시 종료되지 않고 우회(스킵)할 수 있도록 허용합니다.
        .faultTolerant()
        // 6. 예외가 터지면 해당 청크를 롤백하고 1건씩 다시 실행하여, 에러가 난 특정 아이템만 최대 5번까지 건너뜁니다.
        .skipLimit(5)
        .skip(RuntimeException.class)
        .build();
  }

  /** TMDB TV 시리즈 데이터를 읽고, 변환하고, 저장하는 단계를 구성합니다. 메서드 체이닝 순서대로 다음과 같이 동작합니다. */
  @Bean
  public Step tmdbTvStep() {
    return new StepBuilder("tmdbTvStep", jobRepository)
        // 1. 한번에 트랜잭션을 걸어 처리할 단위(20건)를 지정합니다.
        .<TmdbContentDto, Content>chunk(20, transactionManager)
        // 2. TMDB API로부터 인기 TV 시리즈 데이터를 페이지 단위로 지연 로딩 공급합니다.
        .reader(tmdbTvReader)
        // 3. 공급받은 DTO의 데이터를 검증하고 엔티티로 변환합니다.
        .processor(tmdbItemProcessor)
        // 4. 변환된 20건을 모아 중복을 제거한 뒤 DB에 한꺼번에 저장합니다.
        .writer(tmdbItemWriter)
        // 5. 예외 발생 시 배치가 즉시 종료되지 않고 우회(스킵)할 수 있도록 허용합니다.
        .faultTolerant()
        // 6. 예외가 터지면 해당 청크를 롤백하고 1건씩 다시 실행하여, 에러가 난 특정 아이템만 최대 5번까지 건너뜁니다.
        .skipLimit(5)
        .skip(RuntimeException.class)
        .build();
  }
}
