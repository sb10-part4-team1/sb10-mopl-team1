package com.sb10.mopl.batch.job;

import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.listener.MoplBatchMetricsListener;
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

/* SportsDB 경기 콘텐츠를 수집하기 위한 Spring Batch Job 설정 클래스입니다. */
@Configuration
@RequiredArgsConstructor
public class SportsJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SportsItemProcessor sportsItemProcessor;
  private final ContentItemWriter contentItemWriter;
  private final SportsItemReader sportsItemReader;
  private final MoplBatchMetricsListener metricsListener; // 메트릭 리스너 주입

  /* 스포츠 경기 수집 배치를 수행하는 최상위 Job 객체를 정의합니다. */
  @Bean
  public Job sportsJob() {
    return new JobBuilder("sportsJob", jobRepository)
        .start(sportsStep())
        .listener(metricsListener) // Job 리스너 등록
        .build();
  }

  /* 스포츠 경기를 읽고 가공하여 DB에 저장하는 스텝을 구성합니다. */
  @Bean
  public Step sportsStep() {
    return new StepBuilder("sportsStep", jobRepository)
        .<SportsContentDto, Content>chunk(10, transactionManager)
        .reader(sportsItemReader)
        .processor(sportsItemProcessor)
        .writer(contentItemWriter)
        .faultTolerant()
        .skipLimit(5)
        .skip(ContentException.class)
        .listener(metricsListener) // Step 리스너 등록
        .build();
  }
}
