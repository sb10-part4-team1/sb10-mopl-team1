package com.sb10.mopl.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.auth.security.principal.CurrentUser;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import com.sb10.mopl.follow.exception.FollowErrorCode;
import com.sb10.mopl.follow.exception.FollowException;
import com.sb10.mopl.follow.service.FollowService;
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
    controllers = FollowController.class,
    excludeAutoConfiguration = {
      OAuth2ClientWebSecurityAutoConfiguration.class,
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import({GlobalExceptionHandler.class, FollowControllerTest.TestCurrentUserConfig.class})
class FollowControllerTest {

  private static final String BASE_URL = "/api/follows";
  private static final String DETAIL_URL = "/api/follows/{followId}";
  private static final String FOLLOWED_BY_ME_URL = "/api/follows/followed-by-me";
  private static final String COUNT_URL = "/api/follows/count";

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private FollowService followService;

  private UUID followId;
  private UUID followeeId;
  private FollowRequest request;
  private FollowDto followDto;

  @BeforeEach
  void setUp() {
    followId = UUID.randomUUID();
    followeeId = UUID.randomUUID();
    request = new FollowRequest(followeeId);
    followDto = new FollowDto(followId, CURRENT_USER_ID, followeeId);
  }

  @Test
  @DisplayName("팔로우 요청이 유효하면 201 Created와 FollowDto를 반환한다")
  void follow_returnCreated_whenRequestIsValid() throws Exception {
    when(followService.follow(eq(CURRENT_USER_ID), any(FollowRequest.class))).thenReturn(followDto);

    ResultActions resultActions =
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

    resultActions
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(followId.toString()))
        .andExpect(jsonPath("$.followerId").value(CURRENT_USER_ID.toString()))
        .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));

    verify(followService).follow(eq(CURRENT_USER_ID), any(FollowRequest.class));
  }

  @Test
  @DisplayName("팔로우 요청 시 팔로우 대상자가 없으면 400 Bad Request를 반환한다")
  void follow_returnBadRequest_whenFolloweeIdIsNull() throws Exception {
    FollowRequest invalidRequest = new FollowRequest(null);

    ResultActions resultActions =
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(toJson(invalidRequest)));

    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("이미 팔로우한 사용자를 다시 팔로우하면 409 Conflict를 반환한다")
  void follow_returnConflict_whenFollowAlreadyExists() throws Exception {
    when(followService.follow(eq(CURRENT_USER_ID), any(FollowRequest.class)))
        .thenThrow(
            new FollowException(
                FollowErrorCode.FOLLOW_ALREADY_EXISTS,
                Map.of("followerId", CURRENT_USER_ID, "followeeId", followeeId)));

    ResultActions resultActions =
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

    resultActions
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(FollowErrorCode.FOLLOW_ALREADY_EXISTS.getCode()));
  }

  @Test
  @DisplayName("언팔로우 요청이 유효하면 204 No Content를 반환한다")
  void unfollow_returnNoContent_whenFollowExists() throws Exception {
    ResultActions resultActions = mockMvc.perform(delete(DETAIL_URL, followId));

    resultActions.andExpect(status().isNoContent());

    verify(followService).unfollow(CURRENT_USER_ID, followId);
  }

  @Test
  @DisplayName("존재하지 않는 팔로우 삭제 요청 시 404 Not Found를 반환한다")
  void unfollow_returnNotFound_whenFollowDoesNotExist() throws Exception {
    doThrow(new FollowException(FollowErrorCode.FOLLOW_NOT_FOUND, Map.of("followId", followId)))
        .when(followService)
        .unfollow(CURRENT_USER_ID, followId);

    ResultActions resultActions = mockMvc.perform(delete(DETAIL_URL, followId));

    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(FollowErrorCode.FOLLOW_NOT_FOUND.getCode()));
  }

  @Test
  @DisplayName("본인 소유가 아닌 팔로우 삭제 요청 시 403 Forbidden을 반환한다")
  void unfollow_returnForbidden_whenNotOwner() throws Exception {
    doThrow(
            new FollowException(
                FollowErrorCode.UNAUTHORIZED_FOLLOW_ACCESS,
                Map.of("followId", followId, "userId", CURRENT_USER_ID)))
        .when(followService)
        .unfollow(CURRENT_USER_ID, followId);

    ResultActions resultActions = mockMvc.perform(delete(DETAIL_URL, followId));

    resultActions
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(FollowErrorCode.UNAUTHORIZED_FOLLOW_ACCESS.getCode()));
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중이면 200 OK와 FollowDto를 반환한다")
  void findFollowedByMe_returnOk_whenFollowExists() throws Exception {
    when(followService.findFollowedByMe(CURRENT_USER_ID, followeeId)).thenReturn(followDto);

    ResultActions resultActions =
        mockMvc.perform(get(FOLLOWED_BY_ME_URL).param("followeeId", followeeId.toString()));

    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(followId.toString()))
        .andExpect(jsonPath("$.followerId").value(CURRENT_USER_ID.toString()))
        .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우하지 않았으면 404 Not Found를 반환한다")
  void findFollowedByMe_returnNotFound_whenFollowDoesNotExist() throws Exception {
    when(followService.findFollowedByMe(CURRENT_USER_ID, followeeId))
        .thenThrow(
            new FollowException(
                FollowErrorCode.FOLLOW_NOT_FOUND,
                Map.of("followerId", CURRENT_USER_ID, "followeeId", followeeId)));

    ResultActions resultActions =
        mockMvc.perform(get(FOLLOWED_BY_ME_URL).param("followeeId", followeeId.toString()));

    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(FollowErrorCode.FOLLOW_NOT_FOUND.getCode()));
  }

  @Test
  @DisplayName("특정 유저의 팔로워 수 조회 요청 시 200 OK와 팔로워 수를 반환한다")
  void countFollowers_returnOk() throws Exception {
    when(followService.countFollowers(followeeId)).thenReturn(3L);

    ResultActions resultActions =
        mockMvc.perform(get(COUNT_URL).param("followeeId", followeeId.toString()));

    resultActions.andExpect(status().isOk()).andExpect(jsonPath("$").value(3));
  }

  private String toJson(Object request) throws Exception {
    return objectMapper.writeValueAsString(request);
  }

  private void expectBadRequestWithSystemInputError(ResultActions resultActions) throws Exception {
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));
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
