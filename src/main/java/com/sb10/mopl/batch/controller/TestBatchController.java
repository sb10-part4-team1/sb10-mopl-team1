package com.sb10.mopl.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 개발 및 테스트 목적의 임시 배치 실행용 컨트롤러입니다. */
@Slf4j
@RestController
@RequestMapping("/api/test/batch")
@RequiredArgsConstructor
public class TestBatchController {

  private final JobLauncher jobLauncher;
  private final Job tmdbJob;
  private final Job sportsJob;

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

  /**
   * SportsDB 경기 수집 배치 잡을 즉시 실행합니다.
   *
   * @param targetDate 수동 지정 수집 대상 날짜 (포맷: YYYY-MM-DD, 선택값)
   * @return 잡 실행 결과 메시지
   */
  @PostMapping("/sports")
  public ResponseEntity<String> triggerSportsJob(
      @RequestParam(value = "targetDate", required = false) String targetDate) {
    try {
      log.info("HTTP 요청에 의한 Sports 수집 배치 잡 트리거 시작 - targetDate: {}", targetDate);
      JobParametersBuilder paramsBuilder =
          new JobParametersBuilder().addLong("time", System.currentTimeMillis());

      if (StringUtils.hasText(targetDate)) {
        paramsBuilder.addString("targetDate", targetDate);
      }

      jobLauncher.run(sportsJob, paramsBuilder.toJobParameters());
      return ResponseEntity.ok("Sports 배치 잡이 성공적으로 시작 및 실행 완료되었습니다.");
    } catch (Exception e) {
      log.error("Sports 배치 잡 실행 중 예외 발생", e);
      return ResponseEntity.internalServerError().body("배치 실행 실패: " + e.getMessage());
    }
  }
}
