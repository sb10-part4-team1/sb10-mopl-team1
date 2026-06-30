package com.sb10.mopl.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 개발 및 테스트 목적의 임시 배치 실행용 컨트롤러입니다. */
@Slf4j
@RestController
@RequestMapping("/api/test/batch")
@RequiredArgsConstructor
public class TestBatchController {

  private final JobLauncher jobLauncher;
  private final Job tmdbJob;

  /**
   * TMDB 인기 콘텐츠 수집 배치 잡을 즉시 실행합니다.
   *
   * @return 잡 실행 결과 메시지
   */
  @PostMapping("/tmdb")
  public ResponseEntity<String> triggerTmdbJob() {
    try {
      log.info("HTTP 요청에 의한 TMDB 수집 배치 잡 트리거 시작");
      jobLauncher.run(
          tmdbJob,
          new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters());
      return ResponseEntity.ok("TMDB 배치 잡이 성공적으로 시작 및 실행 완료되었습니다.");
    } catch (Exception e) {
      log.error("TMDB 배치 잡 실행 중 예외 발생", e);
      return ResponseEntity.internalServerError().body("배치 실행 실패: " + e.getMessage());
    }
  }
}
