package com.sb10.mopl.batch.service;

import com.sb10.mopl.batch.exception.BatchErrorCode;
import com.sb10.mopl.batch.exception.BatchException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

/*
 * 배치 관리 전담 비즈니스 서비스 클래스입니다.
 * 자동 복구 스케줄러와 관리자 API의 중복 기동 방지, FAILED 상태 검증 등의 공통 코드를 격리 및 재사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchAdminService {

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job sportsJob;
  private final Job tmdbJob;

  /**
   * 해당 Job의 중복 기동 여부를 확인하고, 가장 최근에 FAILED된 실행 정보를 검증 후 가져옵니다.
   *
   * @param jobName 배치 Job 이름 (sportsJob | tmdbJob)
   * @return 가장 최근 실패한 JobExecution (복구 대상이 아니거나 이력이 없으면 null 반환)
   * @throws BatchException 현재 배치가 이미 실행 중(STARTED)인 경우
   */
  public JobExecution validateAndGetLastFailedExecution(String jobName) {
    // 1. 중복 기동 방지 검증 (STARTED 상태 확인)
    Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(jobName);
    if (!runningExecutions.isEmpty()) {
      throw new BatchException(
          BatchErrorCode.JOB_ALREADY_RUNNING,
          Map.of("jobName", jobName, "message", "해당 배치 Job이 현재 이미 실행 중입니다."));
    }

    // 2. 가장 최근 실행 인스턴스 1건 조회
    List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 1);
    if (instances.isEmpty()) { // 잡 인스턴스가 실행된 적 없으면 리턴
      return null;
    }

    List<JobExecution> executions = jobExplorer.getJobExecutions(instances.get(0));
    if (executions.isEmpty()) { // 잡이 실행되었지만 한번도 Executions(실제 실행 단위)가 없으면 리턴
      return null;
    }

    JobExecution lastExecution = executions.get(0); // 가장 최근 실행된 Execution을 가져옴

    // 3. FAILED 상태 검증
    if (lastExecution.getStatus() != BatchStatus.FAILED) {
      return null;
    }

    return lastExecution;
  }

  /**
   * [수동 재시작 전용] 관리자가 실패 상태인 배치를 강제로 이어서 재시작합니다.
   *
   * @param jobName 재시작할 Job 이름 (sportsJob | tmdbJob)
   */
  public void restartJob(String jobName) {
    Job job = resolveJob(jobName);
    if (job == null) {
      throw new BatchException(
          BatchErrorCode.JOB_NOT_FOUND,
          Map.of("jobName", jobName, "message", "존재하지 않거나 알 수 없는 배치 Job 이름입니다."));
    }

    JobExecution lastExecution = validateAndGetLastFailedExecution(jobName);
    if (lastExecution == null) {
      throw new BatchException(
          BatchErrorCode.JOB_RESTART_UNAVAILABLE,
          Map.of("jobName", jobName, "message", "재시작 가능한 FAILED 상태의 배치 이력이 존재하지 않습니다."));
    }

    try {
      log.info("[ADMIN] {} 배치 관리자 수동 재시작 진행", jobName);
      jobLauncher.run(job, lastExecution.getJobParameters());
    } catch (JobExecutionAlreadyRunningException e) {
      throw new BatchException(
          BatchErrorCode.JOB_ALREADY_RUNNING,
          Map.of("jobName", jobName, "message", "해당 배치 Job이 현재 이미 실행 중입니다."),
          e);
    } catch (JobInstanceAlreadyCompleteException
        | JobRestartException
        | JobParametersInvalidException e) {
      throw new BatchException(
          BatchErrorCode.JOB_RESTART_UNAVAILABLE,
          Map.of("jobName", jobName, "message", "배치 설정 또는 인스턴스 정책에 의해 재시작이 불가능합니다."),
          e);
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
