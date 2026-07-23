package com.sb10.mopl.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.auth.security.principal.CurrentUser;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.common.pagination.SortDirectionConverter;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.review.dto.ReviewAuthorDto;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.dto.ReviewUpdateRequest;
import com.sb10.mopl.review.exception.ReviewErrorCode;
import com.sb10.mopl.review.exception.ReviewException;
import com.sb10.mopl.review.service.ReviewService;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
    controllers = ReviewController.class,
    excludeAutoConfiguration = {
      OAuth2ClientWebSecurityAutoConfiguration.class,
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import({
  GlobalExceptionHandler.class,
  SortDirectionConverter.class,
  ReviewControllerTest.TestCurrentUserConfig.class
})
class ReviewControllerTest {

  private static final String REVIEW_URL = "/api/reviews";
  private static final String REVIEW_DETAIL_URL = "/api/reviews/{reviewId}";

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ReviewService reviewService;

  private UUID contentId;
  private UUID reviewId;
  private ReviewDto reviewDto;

  @BeforeEach
  void setUp() {
    contentId = UUID.randomUUID();
    reviewId = UUID.randomUUID();

    reviewDto =
        new ReviewDto(
            reviewId,
            contentId,
            new ReviewAuthorDto(CURRENT_USER_ID, "테스트 사용자", null),
            "좋은 콘텐츠입니다.",
            5);
  }

  @Test
  @DisplayName("리뷰 생성 요청이 유효하면 201 Created와 ReviewDto를 반환한다")
  void create_returnCreated_whenRequestIsValid() throws Exception {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    when(reviewService.create(request, CURRENT_USER_ID)).thenReturn(reviewDto);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.contentId").value(contentId.toString()))
        .andExpect(jsonPath("$.author.userId").value(CURRENT_USER_ID.toString()))
        .andExpect(jsonPath("$.author.name").value("테스트 사용자"))
        .andExpect(jsonPath("$.text").value("좋은 콘텐츠입니다."))
        .andExpect(jsonPath("$.rating").value(5));

    verify(reviewService).create(request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 콘텐츠 ID가 없으면 400 Bad Request를 반환한다")
  void create_returnBadRequest_whenContentIdIsNull() throws Exception {
    // given
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.putNull("contentId");
    requestBody.put("text", "좋은 콘텐츠입니다.");
    requestBody.put("rating", 5);

    String requestJson = objectMapper.writeValueAsString(requestBody);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL).contentType(MediaType.APPLICATION_JSON).content(requestJson));

    // then
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));

    verify(reviewService, never()).create(any(), any());
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 리뷰 내용이 비어 있으면 400 Bad Request를 반환한다")
  void create_returnBadRequest_whenTextIsBlank() throws Exception {
    // given
    String requestJson =
        objectMapper.writeValueAsString(Map.of("contentId", contentId, "text", " ", "rating", 5));

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL).contentType(MediaType.APPLICATION_JSON).content(requestJson));

    // then
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));

    verify(reviewService, never()).create(any(), any());
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 평점이 범위를 벗어나면 400 Bad Request를 반환한다")
  void create_returnBadRequest_whenRatingIsInvalid() throws Exception {
    // given
    String requestJson =
        objectMapper.writeValueAsString(
            Map.of("contentId", contentId, "text", "좋은 콘텐츠입니다.", "rating", 6));

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL).contentType(MediaType.APPLICATION_JSON).content(requestJson));

    // then
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));

    verify(reviewService, never()).create(any(), any());
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 콘텐츠가 없으면 404 Not Found를 반환한다")
  void create_returnNotFound_whenContentDoesNotExist() throws Exception {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    when(reviewService.create(request, CURRENT_USER_ID)).thenThrow(contentNotFoundException());

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ContentErrorCode.CONTENT_NOT_FOUND.getCode()));

    verify(reviewService).create(request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 사용자가 없으면 404 Not Found를 반환한다")
  void create_returnNotFound_whenUserDoesNotExist() throws Exception {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    when(reviewService.create(request, CURRENT_USER_ID)).thenThrow(userNotFoundException());

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(UserErrorCode.USER_NOT_FOUND.getCode()));

    verify(reviewService).create(request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 이미 리뷰를 작성했으면 409 Conflict를 반환한다")
  void create_returnConflict_whenReviewAlreadyExists() throws Exception {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    when(reviewService.create(request, CURRENT_USER_ID)).thenThrow(reviewAlreadyExistsException());

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(REVIEW_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ReviewErrorCode.REVIEW_ALREADY_EXISTS.getCode()));

    verify(reviewService).create(request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 목록 조회 요청이 유효하면 200 OK와 CursorPageResponse를 반환한다")
  void findAll_returnOk_whenRequestIsValid() throws Exception {
    // given
    CursorPageResponse<ReviewDto> response =
        new CursorPageResponse<>(
            List.of(reviewDto), null, null, false, 1L, "createdAt", SortDirection.DESCENDING);

    when(reviewService.findAll(
            eq(contentId),
            isNull(),
            isNull(),
            eq(10),
            eq("createdAt"),
            eq(SortDirection.DESCENDING)))
        .thenReturn(response);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            get(REVIEW_URL)
                .param("contentId", contentId.toString())
                .param("limit", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "DESC"));

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(reviewId.toString()))
        .andExpect(jsonPath("$.data[0].contentId").value(contentId.toString()))
        .andExpect(jsonPath("$.data[0].author.userId").value(CURRENT_USER_ID.toString()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.sortBy").value("createdAt"));

    verify(reviewService).findAll(contentId, null, null, 10, "createdAt", SortDirection.DESCENDING);
  }

  @Test
  @DisplayName("리뷰 목록 조회 실패 - 지원하지 않는 정렬 기준이면 400 Bad Request를 반환한다")
  void findAll_returnBadRequest_whenSortByIsInvalid() throws Exception {
    // given
    when(reviewService.findAll(
            eq(contentId), isNull(), isNull(), eq(10), eq("rating"), eq(SortDirection.DESCENDING)))
        .thenThrow(invalidSortByException());

    // when
    ResultActions resultActions =
        mockMvc.perform(
            get(REVIEW_URL)
                .param("contentId", contentId.toString())
                .param("limit", "10")
                .param("sortBy", "rating")
                .param("sortDirection", "DESC"));

    // then
    resultActions
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ReviewErrorCode.INVALID_REVIEW_VALUE.getCode()));

    verify(reviewService).findAll(contentId, null, null, 10, "rating", SortDirection.DESCENDING);
  }

  @Test
  @DisplayName("리뷰 수정 요청이 유효하면 200 OK와 ReviewDto를 반환한다")
  void update_returnOk_whenRequestIsValid() throws Exception {
    // given
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 4);

    ReviewDto updatedReviewDto =
        new ReviewDto(
            reviewId,
            contentId,
            new ReviewAuthorDto(CURRENT_USER_ID, "테스트 사용자", null),
            "수정된 리뷰",
            4);

    when(reviewService.update(reviewId, request, CURRENT_USER_ID)).thenReturn(updatedReviewDto);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(REVIEW_DETAIL_URL, reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.contentId").value(contentId.toString()))
        .andExpect(jsonPath("$.text").value("수정된 리뷰"))
        .andExpect(jsonPath("$.rating").value(4));

    verify(reviewService).update(reviewId, request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 리뷰 내용이 비어 있으면 400 Bad Request를 반환한다")
  void update_returnBadRequest_whenTextIsBlank() throws Exception {
    // given
    String requestJson = objectMapper.writeValueAsString(Map.of("text", " ", "rating", 4));

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(REVIEW_DETAIL_URL, reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

    // then
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));

    verify(reviewService, never()).update(any(), any(), any());
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 평점이 범위를 벗어나면 400 Bad Request를 반환한다")
  void update_returnBadRequest_whenRatingIsInvalid() throws Exception {
    // given
    String requestJson = objectMapper.writeValueAsString(Map.of("text", "수정된 리뷰", "rating", 0));

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(REVIEW_DETAIL_URL, reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

    // then
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));

    verify(reviewService, never()).update(any(), any(), any());
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 리뷰가 없으면 404 Not Found를 반환한다")
  void update_returnNotFound_whenReviewDoesNotExist() throws Exception {
    // given
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 4);

    when(reviewService.update(reviewId, request, CURRENT_USER_ID))
        .thenThrow(reviewNotFoundException());

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(REVIEW_DETAIL_URL, reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ReviewErrorCode.REVIEW_NOT_FOUND.getCode()));

    verify(reviewService).update(reviewId, request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 작성자가 아니면 403 Forbidden을 반환한다")
  void update_returnForbidden_whenUserIsNotAuthor() throws Exception {
    // given
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 4);

    when(reviewService.update(reviewId, request, CURRENT_USER_ID))
        .thenThrow(unauthorizedReviewException());

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(REVIEW_DETAIL_URL, reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS.getCode()));

    verify(reviewService).update(reviewId, request, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 삭제 요청이 유효하면 204 No Content를 반환한다")
  void delete_returnNoContent_whenRequestIsValid() throws Exception {
    // when
    ResultActions resultActions = mockMvc.perform(delete(REVIEW_DETAIL_URL, reviewId));

    // then
    resultActions.andExpect(status().isNoContent());

    verify(reviewService).delete(reviewId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 삭제 실패 - 리뷰가 없으면 404 Not Found를 반환한다")
  void delete_returnNotFound_whenReviewDoesNotExist() throws Exception {
    // given
    doThrow(reviewNotFoundException()).when(reviewService).delete(reviewId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(delete(REVIEW_DETAIL_URL, reviewId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ReviewErrorCode.REVIEW_NOT_FOUND.getCode()));

    verify(reviewService).delete(reviewId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("리뷰 삭제 실패 - 작성자가 아니면 403 Forbidden을 반환한다")
  void delete_returnForbidden_whenUserIsNotAuthor() throws Exception {
    // given
    doThrow(unauthorizedReviewException()).when(reviewService).delete(reviewId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(delete(REVIEW_DETAIL_URL, reviewId));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS.getCode()));

    verify(reviewService).delete(reviewId, CURRENT_USER_ID);
  }

  private ContentException contentNotFoundException() {
    return new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
  }

  private UserException userNotFoundException() {
    return new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", CURRENT_USER_ID));
  }

  private ReviewException reviewAlreadyExistsException() {
    return new ReviewException(
        ReviewErrorCode.REVIEW_ALREADY_EXISTS,
        Map.of("contentId", contentId, "userId", CURRENT_USER_ID));
  }

  private ReviewException reviewNotFoundException() {
    return new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND, Map.of("reviewId", reviewId));
  }

  private ReviewException unauthorizedReviewException() {
    return new ReviewException(
        ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS,
        Map.of("reviewId", reviewId, "userId", CURRENT_USER_ID));
  }

  private ReviewException invalidSortByException() {
    return new ReviewException(
        ReviewErrorCode.INVALID_REVIEW_VALUE, Map.of("sortBy", "지원하지 않는 정렬 기준입니다."));
  }

  @TestConfiguration
  static class TestCurrentUserConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {

      resolvers.add(
          new HandlerMethodArgumentResolver() {

            @Override
            public boolean supportsParameter(@NonNull MethodParameter parameter) {
              return parameter.hasParameterAnnotation(CurrentUser.class)
                  && parameter.getParameterType().equals(AuthenticatedUser.class);
            }

            @Override
            public Object resolveArgument(
                @NonNull MethodParameter parameter,
                @Nullable ModelAndViewContainer mavContainer,
                @NonNull NativeWebRequest webRequest,
                @Nullable WebDataBinderFactory binderFactory) {

              AuthenticatedUser currentUser = mock(AuthenticatedUser.class);
              when(currentUser.id()).thenReturn(CURRENT_USER_ID);

              return currentUser;
            }
          });
    }
  }
}
