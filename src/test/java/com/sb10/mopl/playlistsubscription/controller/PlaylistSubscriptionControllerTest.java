package com.sb10.mopl.playlistsubscription.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.auth.security.principal.CurrentUser;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionErrorCode;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionException;
import com.sb10.mopl.playlistsubscription.service.PlaylistSubscriptionService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
    controllers = PlaylistSubscriptionController.class,
    excludeAutoConfiguration = {
      OAuth2ClientWebSecurityAutoConfiguration.class,
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import({
  GlobalExceptionHandler.class,
  PlaylistSubscriptionControllerTest.TestCurrentUserConfig.class
})
class PlaylistSubscriptionControllerTest {

  private static final String SUBSCRIPTION_URL = "/api/playlists/{playlistId}/subscription";

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PlaylistSubscriptionService playlistSubscriptionService;

  private UUID playlistId;

  @BeforeEach
  void setUp() {
    playlistId = UUID.randomUUID();
  }

  @Test
  @DisplayName("플레이리스트 구독 요청이 유효하면 204 No Content를 반환한다")
  void subscribe_returnNoContent_whenRequestIsValid() throws Exception {
    // when
    ResultActions resultActions = mockMvc.perform(post(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions.andExpect(status().isNoContent());

    verify(playlistSubscriptionService).subscribe(CURRENT_USER_ID, playlistId);
  }

  @Test
  @DisplayName("플레이리스트 구독 실패 - 구독 요청자가 없으면 404 Not Found를 반환한다")
  void subscribe_returnNotFound_whenSubscriberDoesNotExist() throws Exception {
    // given
    doThrow(userNotFoundException())
        .when(playlistSubscriptionService)
        .subscribe(CURRENT_USER_ID, playlistId);

    // when
    ResultActions resultActions = mockMvc.perform(post(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(UserErrorCode.USER_NOT_FOUND.getCode()));

    verify(playlistSubscriptionService).subscribe(CURRENT_USER_ID, playlistId);
  }

  @Test
  @DisplayName("플레이리스트 구독 실패 - 플레이리스트가 없으면 404 Not Found를 반환한다")
  void subscribe_returnNotFound_whenPlaylistDoesNotExist() throws Exception {
    // given
    doThrow(playlistNotFoundException())
        .when(playlistSubscriptionService)
        .subscribe(CURRENT_USER_ID, playlistId);

    // when
    ResultActions resultActions = mockMvc.perform(post(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getCode()));

    verify(playlistSubscriptionService).subscribe(CURRENT_USER_ID, playlistId);
  }

  @Test
  @DisplayName("플레이리스트 구독 실패 - 본인 소유의 플레이리스트이면 403 Forbidden을 반환한다")
  void subscribe_returnForbidden_whenPlaylistIsOwnedBySubscriber() throws Exception {
    // given
    doThrow(unauthorizedException())
        .when(playlistSubscriptionService)
        .subscribe(CURRENT_USER_ID, playlistId);

    // when
    ResultActions resultActions = mockMvc.perform(post(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code")
                .value(
                    PlaylistSubscriptionErrorCode.UNAUTHORIZED_PLAYLIST_SUBSCRIPTION_ACCESS
                        .getCode()));

    verify(playlistSubscriptionService).subscribe(CURRENT_USER_ID, playlistId);
  }

  @Test
  @DisplayName("플레이리스트 구독 실패 - 이미 구독한 플레이리스트이면 409 Conflict를 반환한다")
  void subscribe_returnConflict_whenSubscriptionAlreadyExists() throws Exception {
    // given
    doThrow(alreadyExistsException())
        .when(playlistSubscriptionService)
        .subscribe(CURRENT_USER_ID, playlistId);

    // when
    ResultActions resultActions = mockMvc.perform(post(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.code")
                .value(
                    PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS.getCode()));

    verify(playlistSubscriptionService).subscribe(CURRENT_USER_ID, playlistId);
  }

  @Test
  @DisplayName("플레이리스트 구독 취소 요청이 유효하면 204 No Content를 반환한다")
  void unsubscribe_returnNoContent_whenRequestIsValid() throws Exception {
    // when
    ResultActions resultActions = mockMvc.perform(delete(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions.andExpect(status().isNoContent());

    verify(playlistSubscriptionService).unsubscribe(CURRENT_USER_ID, playlistId);
  }

  @Test
  @DisplayName("플레이리스트 구독 취소 실패 - 구독 관계가 없으면 404 Not Found를 반환한다")
  void unsubscribe_returnNotFound_whenSubscriptionDoesNotExist() throws Exception {
    // given
    doThrow(subscriptionNotFoundException())
        .when(playlistSubscriptionService)
        .unsubscribe(CURRENT_USER_ID, playlistId);

    // when
    ResultActions resultActions = mockMvc.perform(delete(SUBSCRIPTION_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.code")
                .value(PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND.getCode()));

    verify(playlistSubscriptionService).unsubscribe(CURRENT_USER_ID, playlistId);
  }

  private UserException userNotFoundException() {
    return new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("subscriberId", CURRENT_USER_ID));
  }

  private PlaylistException playlistNotFoundException() {
    return new PlaylistException(
        PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId));
  }

  private PlaylistSubscriptionException unauthorizedException() {
    return new PlaylistSubscriptionException(
        PlaylistSubscriptionErrorCode.UNAUTHORIZED_PLAYLIST_SUBSCRIPTION_ACCESS,
        Map.of("subscriberId", CURRENT_USER_ID, "playlistId", playlistId));
  }

  private PlaylistSubscriptionException alreadyExistsException() {
    return new PlaylistSubscriptionException(
        PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS,
        Map.of("subscriberId", CURRENT_USER_ID, "playlistId", playlistId));
  }

  private PlaylistSubscriptionException subscriptionNotFoundException() {
    return new PlaylistSubscriptionException(
        PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND,
        Map.of("subscriberId", CURRENT_USER_ID, "playlistId", playlistId));
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
