package com.sb10.mopl.batch.controller;

import com.sb10.mopl.batch.service.BatchAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 관리자 API 컨트롤러입니다.
 *
 * <p>비즈니스 로직(중복 기동 방지, 상태 검증 등)은 BatchAdminService에 전적으로 위임하고, HTTP 응답 결과 처리만 매핑합니다. 예외 발생 시
 * GlobalExceptionHandler가 가로채어 예쁜 에러 JSON 포맷(ErrorResponse)으로 응답합니다.
 *
 * <p>TODO: [Security] 해당 엔드포인트는 ADMIN 권한 인가 처리 필요 (SecurityConfig에 hasRole("ADMIN") 추가 예정)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class BatchAdminController {

  private final BatchAdminService batchAdminService;

  /**
   * 실패한 배치를 관리자가 수동으로 실패 지점부터 이어서 재시작합니다.
   *
   * @param jobName 재시작할 Job 이름 (sportsJob | tmdbJob)
   */
  @PostMapping("/{jobName}/restart")
  public ResponseEntity<String> restartJob(@PathVariable String jobName) {
    batchAdminService.restartJob(jobName);
    return ResponseEntity.ok(jobName + " 배치 재시작 완료");
  }
}
