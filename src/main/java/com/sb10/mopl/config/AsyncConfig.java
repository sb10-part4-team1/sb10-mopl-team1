package com.sb10.mopl.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 애플리케이션 비동기 처리를 활성화하고 비동기 스레드 풀에 MdcTaskDecorator를 등록하여 MDC 컨텍스트가 전파되도록 설정하는 구성 클래스입니다. */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer, WebMvcConfigurer {

  @Bean(name = "ioExecutor")
  public ThreadPoolTaskExecutor ioExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 1코어 2스레드 AWS EC2 프리티어(t2.micro/t3.micro - 메모리 1GB 제한) 환경에 최적화된 I/O 바운드 스레드 풀 설정
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("io-worker-");
    // MDC 전파용 테스크 데코레이터 설정
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return ioExecutor();
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    // Spring MVC의 비동기 요청 처리 스레드 풀에도 MDC 데코레이터가 달린 스레드 풀을 구성
    configurer.setTaskExecutor(ioExecutor());
  }
}
