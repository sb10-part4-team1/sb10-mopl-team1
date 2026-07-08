package com.sb10.mopl.batch.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

/*
 * Spring Retry 프레임워크와 연동되어 동작하는 글로벌 리트라이 모니터링 리스너입니다.
 *
 * 외부 API 호출(@Retryable) 혹은 배치 프로세스 중 실패 후 재시도(Retry)가 발생할 때마다
 * 어떤 성격의 리소스를 처리하던 중 예외가 났는지 종류별로 프로메테우스 카운터를 누적합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoplRetryListener implements RetryListener {

  private final MeterRegistry meterRegistry;

  @Override
  public <T, E extends Throwable> void onError(
      RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

    // 현재 수행 중인 태스크 라벨 또는 재시도 컨텍스트 속성명 추출
    String label =
        context.getAttribute(RetryContext.NAME) != null
            ? (String) context.getAttribute(RetryContext.NAME)
            : "api-retry";

    // 카운터 기록: mopl.batch.retry.total
    Counter.builder("mopl.batch.retry.total")
        .description("Total number of retry attempts in batch operations")
        .tags("label", label, "exception", throwable.getClass().getSimpleName())
        .register(meterRegistry)
        .increment();

    log.warn(
        "배치 작업 중 오류 복구를 위한 재시도 발생 - Label: {}, Count: {}, Exception: {}",
        label,
        context.getRetryCount(),
        throwable.getMessage());
  }
}
