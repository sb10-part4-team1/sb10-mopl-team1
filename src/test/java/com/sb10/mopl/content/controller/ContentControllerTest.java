package com.sb10.mopl.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.service.ContentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ContentController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import(GlobalExceptionHandler.class)
class ContentControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ContentService contentService;

  @Test
  @DisplayName("콘텐츠 생성 요청이 유효하면 201 Created와 ContentDto를 반환한다")
  void createContent_returnCreated_whenRequestIsValid() throws Exception {
    // given: Mock DTO 데이터 및 Mock 서비스 응답 구성
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 영화", List.of("SF"));

    UUID mockId = UUID.randomUUID();
    ContentDto mockResponse =
        new ContentDto(
            mockId, "MOVIE", "인셉션", "SF 영화", "/uploads/test.jpg", List.of("SF"), 0.0, 0, 0L);

    when(contentService.createContent(any(), any())).thenReturn(mockResponse);

    String requestJson = objectMapper.writeValueAsString(request);
    MockMultipartFile requestPart =
        new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestJson.getBytes(StandardCharsets.UTF_8));

    MockMultipartFile thumbnailPart =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    // when: 컨트롤러 호출
    var resultActions =
        mockMvc.perform(
            multipart("/api/content")
                .file(requestPart)
                .file(thumbnailPart)
                .contentType(MediaType.MULTIPART_FORM_DATA));

    // then: 201 Created 헤더 및 DTO가 제대로 출력되는지 확인
    resultActions
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(mockId.toString()))
        .andExpect(jsonPath("$.title").value("인셉션"));
  }

  @Test
  @DisplayName("콘텐츠 생성 요청 시 제목이 비어있으면 400 Bad Request를 반환한다")
  void createContent_returnBadRequest_whenTitleIsEmpty() throws Exception {
    // given: 제목이 빈 문자열인 유효하지 않은 DTO 생성
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.MOVIE, "", "SF 영화", List.of("SF"));

    String requestJson = objectMapper.writeValueAsString(request);
    MockMultipartFile requestPart =
        new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestJson.getBytes(StandardCharsets.UTF_8));

    // when: 콘텐츠 생성 API 호출
    var resultActions =
        mockMvc.perform(
            multipart("/api/content").file(requestPart).contentType(MediaType.MULTIPART_FORM_DATA));

    // then: 400 Bad Request 응답 코드를 확인
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));
  }

  @Test
  @DisplayName("콘텐츠 수정 요청이 유효하면 200 OK와 ContentDto를 반환한다")
  void updateContent_returnOk_whenRequestIsValid() throws Exception {
    // given: Mock DTO 데이터 및 Mock 서비스 응답 구성
    UUID mockId = UUID.randomUUID();
    ContentUpdateRequest request =
        new ContentUpdateRequest("인셉션 수정", "SF 영화 수정", List.of("SF", "스릴러"));

    ContentDto mockResponse =
        new ContentDto(
            mockId,
            "MOVIE",
            "인셉션 수정",
            "SF 영화 수정",
            "/uploads/updated.jpg",
            List.of("SF", "스릴러"),
            0.0,
            0,
            0L);

    when(contentService.updateContent(eq(mockId), any(), any())).thenReturn(mockResponse);

    String requestJson = objectMapper.writeValueAsString(request);
    MockMultipartFile requestPart =
        new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestJson.getBytes(StandardCharsets.UTF_8));

    MockMultipartFile thumbnailPart =
        new MockMultipartFile("thumbnail", "updated.jpg", "image/jpeg", "bytes".getBytes());

    // when: 콘텐츠 수정 API 호출
    var resultActions =
        mockMvc.perform(
            multipart("/api/content/" + mockId)
                .file(requestPart)
                .file(thumbnailPart)
                .with(
                    req -> {
                      req.setMethod("PUT");
                      return req;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA));

    // then: 200 OK 헤더 및 DTO 확인
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(mockId.toString()))
        .andExpect(jsonPath("$.title").value("인셉션 수정"))
        .andExpect(jsonPath("$.description").value("SF 영화 수정"))
        .andExpect(jsonPath("$.thumbnailUrl").value("/uploads/updated.jpg"));
  }

  @Test
  @DisplayName("콘텐츠 수정 요청 시 제목이 비어있으면 400 Bad Request를 반환한다")
  void updateContent_returnBadRequest_whenTitleIsEmpty() throws Exception {
    // given: 제목이 빈 문자열인 유효하지 않은 DTO 생성
    UUID mockId = UUID.randomUUID();
    ContentUpdateRequest request = new ContentUpdateRequest("", "SF 영화 수정", List.of("SF", "스릴러"));

    String requestJson = objectMapper.writeValueAsString(request);
    MockMultipartFile requestPart =
        new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestJson.getBytes(StandardCharsets.UTF_8));

    // when: 콘텐츠 수정 API 호출
    var resultActions =
        mockMvc.perform(
            multipart("/api/content/" + mockId)
                .file(requestPart)
                .with(
                    req -> {
                      req.setMethod("PUT");
                      return req;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA));

    // then: 400 Bad Request 응답 코드를 확인
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));
  }

  @Test
  @DisplayName("콘텐츠 삭제 요청이 유효하면 204 No Content를 반환한다")
  void delete_returnNoContent_whenIdIsValid() throws Exception {
    // given: 임의의 콘텐츠 ID 설정
    UUID mockId = UUID.randomUUID();

    // when: 콘텐츠 삭제 API 호출
    var resultActions = mockMvc.perform(delete("/api/content/" + mockId));

    // then: 204 No Content 응답 확인
    resultActions.andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 삭제 요청 시 404 Not Found를 반환한다")
  void delete_returnNotFound_whenContentDoesNotExist() throws Exception {
    // given: 임의의 콘텐츠 ID 설정
    UUID mockId = UUID.randomUUID();

    // when: 콘텐츠 삭제 API 호출
    doThrow(new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", mockId)))
        .when(contentService)
        .deleteContent(mockId);

    var resultActions = mockMvc.perform(delete("/api/content/" + mockId));

    // then: 204 No Content 응답 확인
    resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CT01"));
  }
}
