package com.sb10.mopl.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * Micrometer 메트릭 수집 엔진 글로벌 설정 클래스입니다.
 *
 * 수집되는 모든 커스텀 및 빌트인 지표에 공통 식별용 태그(application = mopl)를 자동으로 삽입해 줍니다.
 */
@Configuration
public class MetricsConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config().commonTags("application", "mopl");
  }
}
