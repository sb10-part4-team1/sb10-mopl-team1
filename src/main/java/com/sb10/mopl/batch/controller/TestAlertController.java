package com.sb10.mopl.batch.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * [임시 테스트용 파일] 디스코드 알림 및 그라파나 경보 검증용 컨트롤러
 * 검증이 끝나면 이 파일(TestAlertController.java)을 통째로 삭제하시면 됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/test/batch/alert")
@RequiredArgsConstructor
public class TestAlertController {

  private final MeterRegistry meterRegistry;
  private final Map<String, Double> mockConsecutiveFailures = new ConcurrentHashMap<>();

  /*
   * 호출할 때마다 지정한 job의 연속 실패 게이지를 1씩 증가시키는 테스트 API
   * 예시: http://localhost/api/test/batch/alert/trigger?jobName=sportsJob
   */
  @GetMapping("/trigger")
  public String triggerFailureGauge(@RequestParam(defaultValue = "sportsJob") String jobName) {
    double current = mockConsecutiveFailures.getOrDefault(jobName, 0.0);
    double next = current + 1.0;
    if (next > 3.0) {
      next = 0.0; /* 3을 초과하면 다시 0으로 리셋 */
    }

    mockConsecutiveFailures.put(jobName, next);

    log.info("[TEST-ALERT-CONTROLLER] {} 실패 게이지 강제 갱신: {} -> {}", jobName, current, next);

    /* 그라파나가 수집해가는 게이지 메트릭에 강제 매핑 */
    meterRegistry.gauge(
        "mopl.batch.recovery.failure.consecutive",
        List.of(Tag.of("jobName", jobName)),
        mockConsecutiveFailures,
        map -> map.getOrDefault(jobName, 0.0));

    return String.format(
        "Job [%s] Consecutive Failure Gauge updated to: %.1f (3.0이 넘으면 알림이 발생합니다.)", jobName, next);
  }
}
