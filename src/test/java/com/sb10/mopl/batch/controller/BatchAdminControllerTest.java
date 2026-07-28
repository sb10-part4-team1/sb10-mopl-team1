package com.sb10.mopl.batch.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.batch.exception.BatchErrorCode;
import com.sb10.mopl.batch.exception.BatchException;
import com.sb10.mopl.batch.service.BatchAdminService;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = BatchAdminController.class,
    excludeAutoConfiguration = {
      OAuth2ClientWebSecurityAutoConfiguration.class,
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import(GlobalExceptionHandler.class)
class BatchAdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BatchAdminService batchAdminService;

  @Nested
  @DisplayName("1. BatchAdminController 배치 재시작 API 정상 호출 검증")
  class SuccessCases {

    @Test
    @DisplayName("유효한 배치 Job 이름으로 요청 시 200 OK와 완료 메시지를 반환한다")
    void restartJob_success_whenValidJobNameProvided() throws Exception {
      // given: batchAdminService.restartJob이 정상 처리되도록 설정한다
      String validJobName = "sportsJob";
      doNothing().when(batchAdminService).restartJob(validJobName);

      // when: POST /api/admin/batch/sportsJob/restart 요청을 수행할 때
      // that: HTTP 200 OK와 성공 문자열이 반환되는지 검증한다
      mockMvc
          .perform(post("/api/admin/batch/{jobName}/restart", validJobName))
          .andExpect(status().isOk())
          .andExpect(content().string("sportsJob 배치 재시작 완료"));
    }
  }

  @Nested
  @DisplayName("2. BatchAdminController 배치 재시작 API 부적절한 파라미터 예외 검증")
  class ExceptionCases {

    @Test
    @DisplayName("존재하지 않거나 공백인 jobName으로 요청 시 JOB_NOT_FOUND 예외 및 400 Bad Request를 반환한다")
    void restartJob_fail_whenInvalidOrBlankJobNameProvided() throws Exception {
      // given: 존재하지 않거나 공백인 jobName 파라미터 준비 및 BatchException(JOB_NOT_FOUND) 발생 모킹
      String invalidJobName = "unknownJob";
      String blankJobName = "   ";

      doThrow(
              new BatchException(
                  BatchErrorCode.JOB_NOT_FOUND,
                  Map.of("jobName", invalidJobName, "message", "존재하지 않거나 알 수 없는 배치 Job 이름입니다.")))
          .when(batchAdminService)
          .restartJob(invalidJobName);

      doThrow(
              new BatchException(
                  BatchErrorCode.JOB_NOT_FOUND,
                  Map.of("jobName", blankJobName, "message", "존재하지 않거나 알 수 없는 배치 Job 이름입니다.")))
          .when(batchAdminService)
          .restartJob(blankJobName);

      // when & that: 존재하지 않는 jobName 요청 시 400 Bad Request 및 에러 코드 BT02(JOB_NOT_FOUND) 검증한다
      mockMvc
          .perform(post("/api/admin/batch/{jobName}/restart", invalidJobName))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BT02"));

      // when & that: 공백 문자열 jobName 요청 시 400 Bad Request 및 에러 코드 BT02(JOB_NOT_FOUND) 검증한다
      mockMvc
          .perform(post("/api/admin/batch/{jobName}/restart", blankJobName))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BT02"));
    }
  }
}
