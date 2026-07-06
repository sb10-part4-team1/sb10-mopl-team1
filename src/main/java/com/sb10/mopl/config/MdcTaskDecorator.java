package com.sb10.mopl.config;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Spring의 TaskExecutor가 작업을 수행할 때 부모 스레드의 MDC 컨텍스트를 자식 스레드로 복사해주는 데코레이터입니다.
 *
 * <p>비동기 스레드 풀 스레드 반환 시 MDC를 깨끗하게 비워 스레드 오염을 방지합니다.
 */
public class MdcTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    // 부모 스레드의 MDC 컨텍스트 복사
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    return () -> {
      if (contextMap != null) {
        MDC.setContextMap(contextMap);
      } else {
        MDC.clear();
      }
      try {
        runnable.run();
      } finally {
        // 실행 완료 후 비동기 스레드 MDC 정리
        MDC.clear();
      }
    };
  }
}
