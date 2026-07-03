package com.sb10.mopl.batch.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * 배치 관리자 API 컨트롤러입니다.
 * 자동 복구가 3회 실패하거나 치명적 에러로 락이 걸린 배치를 관리자가 수동으로 재시작할 수 있는 엔드포인트를 제공합니다. Spring Batch의 Restart 기능으로
 * 실패한 지점부터 이어서 재실행됩니다.
 * TODO: [Security] 해당 엔드포인트는 ADMIN 권한 인가 처리 필요 (SecurityConfig에 hasRole("ADMIN") 추가 예정)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class BatchAdminController {

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job sportsJob;
  private final Job tmdbJob;

  /**
   * 실패한 배치를 관리자가 수동으로 실패 지점부터 이어서 재시작합니다.
   *
   * @param jobName 재시작할 Job 이름 (sportsJob | tmdbJob)
   */
  @PostMapping("/{jobName}/restart")
  public ResponseEntity<String> restartJob(@PathVariable String jobName) {
    Job job = resolveJob(jobName);
    if (job == null) {
      return ResponseEntity.badRequest().body("알 수 없는 배치 Job 이름: " + jobName);
    }

    // 1. 가장 최근 실행 이력을 조회합니다.
    List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 1);
    if (instances.isEmpty()) {
      return ResponseEntity.badRequest().body("실행 이력 없음: " + jobName);
    }

    List<JobExecution> executions = jobExplorer.getJobExecutions(instances.get(0));
    if (executions.isEmpty()) {
      return ResponseEntity.badRequest().body("실행 이력 없음: " + jobName);
    }

    // 2. 가장 최근 실행 상태가 FAILED인지 확인합니다.
    JobExecution lastExecution = executions.get(0);
    if (lastExecution.getStatus() != BatchStatus.FAILED) {
      return ResponseEntity.badRequest()
          .body("FAILED 상태가 아닌 배치는 재시작 불가 - 현재 상태: " + lastExecution.getStatus());
    }

    // 3. 실패했던 파라미터 그대로 재시작 - Spring Batch가 자동으로 실패 지점부터 이어서 재실행합니다.
    try {
      log.info("[ADMIN] {} 배치 관리자 수동 재시작 요청 수신", jobName);
      jobLauncher.run(job, lastExecution.getJobParameters());
      log.info("[ADMIN] {} 배치 관리자 수동 재시작 완료", jobName);
      return ResponseEntity.ok(jobName + " 배치 재시작 완료");
    } catch (Exception e) {
      log.error("[ADMIN] {} 배치 관리자 수동 재시작 실패: {}", jobName, e.getMessage());
      return ResponseEntity.internalServerError().body("재시작 실패: " + e.getMessage());
    }
  }

  private Job resolveJob(String jobName) {
    return switch (jobName) {
      case "sportsJob" -> sportsJob;
      case "tmdbJob" -> tmdbJob;
      default -> null;
    };
  }
}
