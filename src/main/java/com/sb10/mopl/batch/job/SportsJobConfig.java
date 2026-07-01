package com.sb10.mopl.batch.job;

import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.processor.SportsItemProcessor;
import com.sb10.mopl.batch.reader.SportsItemReader;
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

/** SportsDB 경기 콘텐츠를 수집하기 위한 Spring Batch Job 설정 클래스입니다. */
@Configuration
@RequiredArgsConstructor
public class SportsJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SportsItemProcessor sportsItemProcessor;
  private final ContentItemWriter contentItemWriter;
  private final SportsItemReader sportsItemReader;

  /** 스포츠 경기 수집 배치를 수행하는 최상위 Job 객체를 정의합니다. */
  @Bean
  public Job sportsJob() {
    return new JobBuilder("sportsJob", jobRepository)
        // 1. 스포츠 경기 수집을 처리할 메인 스텝을 지정하여 구동 프로세스를 시작합니다.
        .start(sportsStep())
        // 2. 최종 배치 Job 빌드 인스턴스를 완성합니다.
        .build();
  }

  /** 스포츠 경기를 읽고 가공하여 DB에 저장하는 스텝을 구성합니다. */
  @Bean
  public Step sportsStep() {
    return new StepBuilder("sportsStep", jobRepository)
        // 1. 한번에 트랜잭션을 걸어 처리할 단위(10건)와 트랜잭션 매니저를 매핑합니다.
        .<SportsContentDto, Content>chunk(10, transactionManager)
        // 2. SPORTS DB API로부터 모든 리그의 경기 정보를 순회하며 취합 공급하는 리더를 지정합니다.
        .reader(sportsItemReader)
        // 3. 공급받은 DTO의 데이터 유효성을 검증하고 엔티티로 변환합니다.
        .processor(sportsItemProcessor)
        // 4. 중복을 배제하고 최종적으로 신규 콘텐츠만 일괄 영속화합니다.
        .writer(contentItemWriter)
        // 5. 배치 수행 중 특정 예외 발생 시 배치가 중단되지 않고 우회(스킵)할 수 있는 내성 기능을 부여합니다.
        .faultTolerant()
        // 6. 허용할 최대 스킵 횟수를 5회로 지정하여, 그 이상 오류 발생 시 장애 전파를 위해 배치에 예외를 전파합니다.
        .skipLimit(5)
        // 7. 스킵을 적용할 타겟 비즈니스 예외(ContentException)를 지정합니다.
        .skip(ContentException.class)
        // 8. 최종 배치 Step 빌드 인스턴스를 완성합니다.
        .build();
  }
}
