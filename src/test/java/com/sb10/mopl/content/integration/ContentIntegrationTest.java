package com.sb10.mopl.content.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import com.sb10.mopl.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ContentRepository contentRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private EntityManager em;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
  }

  private UserRequestPostProcessor adminUser() {
    return user("admin").authorities(new SimpleGrantedAuthority(UserRole.ADMIN.authorityName()));
  }

  private UserRequestPostProcessor normalUser() {
    return user("user").authorities(new SimpleGrantedAuthority(UserRole.USER.authorityName()));
  }

  @Nested
  @DisplayName("1. 정상 비즈니스 라이프사이클 통합 검증 (성공 케이스)")
  class SuccessCases {

    @Test
    @DisplayName("관리자는 콘텐츠 생성, 상세 조회, 수정, 커서 목록 조회, 삭제 전체 생명주기 흐름을 성공적으로 수행한다")
    void contentLifecycle_success_whenAdminOperatesFullFlow() throws Exception {
      // 1. [생성] 관리자 권한으로 영화 콘텐츠 생성 요청
      ContentCreateRequest createReq =
          new ContentCreateRequest(ContentType.MOVIE, "인터스텔라", "우주 탐사 영화", List.of("SF", "우주"));

      MockMultipartFile requestPart =
          new MockMultipartFile(
              "request",
              "",
              MediaType.APPLICATION_JSON_VALUE,
              objectMapper.writeValueAsString(createReq).getBytes(StandardCharsets.UTF_8));

      MockMultipartFile thumbnail =
          new MockMultipartFile("thumbnail", "interstellar.jpg", "image/jpeg", "bytes".getBytes());

      String responseString =
          mockMvc
              .perform(
                  multipart("/api/contents")
                      .file(requestPart)
                      .file(thumbnail)
                      .with(adminUser())
                      .with(csrf())
                      .contentType(MediaType.MULTIPART_FORM_DATA))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.title").value("인터스텔라"))
              .andExpect(jsonPath("$.type").value("movie"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      UUID createdId = UUID.fromString(objectMapper.readTree(responseString).get("id").asText());
      em.flush();
      em.clear();
      assertThat(contentRepository.findById(createdId)).isPresent();

      // 2. [상세 조회] 생성된 콘텐츠 단건 상세 조회
      mockMvc
          .perform(get("/api/contents/{id}", createdId).with(adminUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(createdId.toString()))
          .andExpect(jsonPath("$.title").value("인터스텔라"))
          .andExpect(jsonPath("$.tags").value(org.hamcrest.Matchers.hasItems("SF", "우주")));

      // 3. [수정] 콘텐츠 제목 및 태그 수정 요청
      ContentUpdateRequest updateReq =
          new ContentUpdateRequest("인터스텔라 (감독판)", "확장판 우주 탐사 영화", List.of("SF", "우주", "명작"));

      MockMultipartFile updateRequestPart =
          new MockMultipartFile(
              "request",
              "",
              MediaType.APPLICATION_JSON_VALUE,
              objectMapper.writeValueAsString(updateReq).getBytes(StandardCharsets.UTF_8));

      mockMvc
          .perform(
              multipart("/api/contents/{id}", createdId)
                  .file(updateRequestPart)
                  .with(adminUser())
                  .with(csrf())
                  .with(
                      req -> {
                        req.setMethod("PATCH");
                        return req;
                      })
                  .contentType(MediaType.MULTIPART_FORM_DATA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("인터스텔라 (감독판)"))
          .andExpect(jsonPath("$.description").value("확장판 우주 탐사 영화"));

      // 4. [목록 조회] 커서 기반 목록 조회
      mockMvc
          .perform(
              get("/api/contents")
                  .param("sortBy", "createdAt")
                  .param("limit", "10")
                  .param("sortDirection", "DESCENDING")
                  .with(adminUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].id").value(createdId.toString()))
          .andExpect(jsonPath("$.totalCount").value(1));

      // 5. [삭제] 콘텐츠 삭제 요청
      mockMvc
          .perform(delete("/api/contents/{id}", createdId).with(adminUser()).with(csrf()))
          .andExpect(status().isNoContent());

      assertThat(contentRepository.findById(createdId)).isEmpty();
    }
  }

  @Nested
  @DisplayName("2. 예측 가능한 실패 검증 (4xx 비즈니스/검증/권한 실패 케이스)")
  class PredictableFailureCases {

    @Test
    @DisplayName("일반 사용자(ROLE_USER)가 콘텐츠 생성 시도 시 403 Forbidden으로 차단된다")
    void createContent_fail_whenUserRoleRequestsCreation() throws Exception {
      // given: 일반 유저 생성 요청 DTO 구성
      ContentCreateRequest createReq =
          new ContentCreateRequest(ContentType.MOVIE, "기생충", "드라마", List.of("스릴러"));

      MockMultipartFile requestPart =
          new MockMultipartFile(
              "request",
              "",
              MediaType.APPLICATION_JSON_VALUE,
              objectMapper.writeValueAsString(createReq).getBytes(StandardCharsets.UTF_8));

      // when & that: 일반 유저(ROLE_USER) 권한으로 생성 호출 시 403 Forbidden 검증한다
      mockMvc
          .perform(
              multipart("/api/contents")
                  .file(requestPart)
                  .with(normalUser())
                  .with(csrf())
                  .contentType(MediaType.MULTIPART_FORM_DATA))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("SYS04"));
    }

    @Test
    @DisplayName("비로그인 익명 사용자가 콘텐츠 삭제 시도 시 401 Unauthorized로 차단된다")
    void deleteContent_fail_whenAnonymousUserRequestsDeletion() throws Exception {
      // when & that: 비로그인 사용자가 삭제 호출 시 401 Unauthorized 검증한다
      mockMvc
          .perform(delete("/api/contents/{id}", UUID.randomUUID()).with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("생성 요청 DTO의 제목이 비어있으면 400 Bad Request를 반환한다")
    void createContent_fail_whenTitleIsBlank() throws Exception {
      // given: 제목이 빈 문자열인 DTO 준비
      ContentCreateRequest createReq =
          new ContentCreateRequest(ContentType.MOVIE, "   ", "설명", List.of());

      MockMultipartFile requestPart =
          new MockMultipartFile(
              "request",
              "",
              MediaType.APPLICATION_JSON_VALUE,
              objectMapper.writeValueAsString(createReq).getBytes(StandardCharsets.UTF_8));

      // when & that: 400 Bad Request 검증한다
      mockMvc
          .perform(
              multipart("/api/contents")
                  .file(requestPart)
                  .with(adminUser())
                  .with(csrf())
                  .contentType(MediaType.MULTIPART_FORM_DATA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("SYS01"));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 ID로 상세 조회 요청 시 404 Not Found를 반환한다")
    void findContent_fail_whenContentDoesNotExist() throws Exception {
      // given: 존재하지 않는 임의의 UUID
      UUID nonexistentId = UUID.randomUUID();

      // when & that: 404 Not Found 검증한다
      mockMvc
          .perform(get("/api/contents/{id}", nonexistentId).with(adminUser()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("CT01"));
    }

    @Test
    @DisplayName("목록 조회 시 잘못된 커서 포맷을 전달하면 400 Bad Request와 INVALID_CURSOR_VALUE(CT06) 에러 코드를 반환한다")
    void findAllContents_fail_whenInvalidCursorProvided() throws Exception {
      // when & that: cursor 정보만 있고 idAfter가 없는 불일치 파라미터 전달 시 400 에러 검증한다
      mockMvc
          .perform(
              get("/api/contents")
                  .param("sortBy", "createdAt")
                  .param("cursor", "invalid-date-string")
                  .param("idAfter", UUID.randomUUID().toString())
                  .param("limit", "10")
                  .with(adminUser()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("CT06"));
    }
  }

  @Nested
  @DisplayName("3. 시스템 및 제약조건 실패 검증 (예측 불가능/예외 격리 케이스)")
  class UnpredictableFailureCases {

    @Test
    @DisplayName("존재하지 않는 정렬 기준 파라미터 입력 시 GlobalExceptionHandler가 이를 포착하여 400 Bad Request를 반환한다")
    void findAllContents_fail_whenUnknownSortByProvided() throws Exception {
      // when & that: 시스템에서 정의되지 않은 sortBy="INVALID_SORT" 파라미터 입력 시 400 에러 포착 검증한다
      mockMvc
          .perform(
              get("/api/contents")
                  .param("sortBy", "INVALID_SORT")
                  .param("limit", "10")
                  .with(adminUser()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("SYS01"));
    }

    @Test
    @DisplayName("Path UUID 파라미터 형식이 올바르지 않으면 400 Bad Request를 반환한다")
    void getContent_fail_whenUuidFormatIsInvalid() throws Exception {
      // when & that: UUID 포맷이 아닌 "not-a-valid-uuid" 경로 파라미터 요청 시 400 에러 검증한다
      mockMvc
          .perform(get("/api/contents/{id}", "not-a-valid-uuid").with(adminUser()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("SYS01"));
    }

    @Test
    @DisplayName("Multipart의 JSON 페이로드가 깨져있으면 HttpMessageNotReadableException이 발생하여 SYS02를 반환한다")
    void createContent_fail_whenMultipartJsonPayloadIsMalformed() throws Exception {
      // given: 형식이 깨진 잘못된 JSON 바이너리 페이로드 준비
      MockMultipartFile malformedPart =
          new MockMultipartFile(
              "request",
              "",
              MediaType.APPLICATION_JSON_VALUE,
              "{ malformed json string ".getBytes(StandardCharsets.UTF_8));

      // when & that: 파싱 실패 시 400 Bad Request (SYS02) 반환되는지 검증한다
      mockMvc
          .perform(
              multipart("/api/contents")
                  .file(malformedPart)
                  .with(adminUser())
                  .with(csrf())
                  .contentType(MediaType.MULTIPART_FORM_DATA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("SYS02"));
    }
  }
}
