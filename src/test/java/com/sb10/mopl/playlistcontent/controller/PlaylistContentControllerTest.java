package com.sb10.mopl.playlistcontent.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentErrorCode;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentException;
import com.sb10.mopl.playlistcontent.service.PlaylistContentService;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    controllers = PlaylistContentController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import({GlobalExceptionHandler.class, PlaylistContentControllerTest.TestCurrentUserConfig.class})
class PlaylistContentControllerTest {

  private static final String CONTENT_URL = "/api/playlists/{playlistId}/contents/{contentId}";

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PlaylistContentService playlistContentService;

  private UUID playlistId;
  private UUID contentId;

  @BeforeEach
  void setUp() {
    playlistId = UUID.randomUUID();
    contentId = UUID.randomUUID();
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 요청이 유효하면 204 No Content를 반환한다")
  void add_returnNoContent_whenRequestIsValid() throws Exception {
    // when
    ResultActions resultActions = mockMvc.perform(post(CONTENT_URL, playlistId, contentId));

    // then
    resultActions.andExpect(status().isNoContent());

    verify(playlistContentService).add(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 플레이리스트가 없으면 404 Not Found를 반환한다")
  void add_returnNotFound_whenPlaylistDoesNotExist() throws Exception {
    // given
    doThrow(playlistNotFoundException())
        .when(playlistContentService)
        .add(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(post(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getCode()));

    verify(playlistContentService).add(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 콘텐츠가 없으면 404 Not Found를 반환한다")
  void add_returnNotFound_whenContentDoesNotExist() throws Exception {
    // given
    doThrow(contentNotFoundException())
        .when(playlistContentService)
        .add(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(post(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ContentErrorCode.CONTENT_NOT_FOUND.getCode()));

    verify(playlistContentService).add(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 소유자가 아니면 403 Forbidden을 반환한다")
  void add_returnForbidden_whenNotOwner() throws Exception {
    // given
    doThrow(unauthorizedException())
        .when(playlistContentService)
        .add(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(post(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code")
                .value(PlaylistContentErrorCode.UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS.getCode()));

    verify(playlistContentService).add(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 이미 추가된 콘텐츠이면 409 Conflict를 반환한다")
  void add_returnConflict_whenPlaylistContentAlreadyExists() throws Exception {
    // given
    doThrow(alreadyExistsException())
        .when(playlistContentService)
        .add(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(post(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.code")
                .value(PlaylistContentErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS.getCode()));

    verify(playlistContentService).add(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 요청이 유효하면 204 No Content를 반환한다")
  void delete_returnNoContent_whenRequestIsValid() throws Exception {
    // when
    ResultActions resultActions = mockMvc.perform(delete(CONTENT_URL, playlistId, contentId));

    // then
    resultActions.andExpect(status().isNoContent());

    verify(playlistContentService).delete(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 실패 - 플레이리스트가 없으면 404 Not Found를 반환한다")
  void delete_returnNotFound_whenPlaylistDoesNotExist() throws Exception {
    // given
    doThrow(playlistNotFoundException())
        .when(playlistContentService)
        .delete(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(delete(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getCode()));

    verify(playlistContentService).delete(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 실패 - 소유자가 아니면 403 Forbidden을 반환한다")
  void delete_returnForbidden_whenNotOwner() throws Exception {
    // given
    doThrow(unauthorizedException())
        .when(playlistContentService)
        .delete(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(delete(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code")
                .value(PlaylistContentErrorCode.UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS.getCode()));

    verify(playlistContentService).delete(playlistId, contentId, CURRENT_USER_ID);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 실패 - 매핑 정보가 없으면 404 Not Found를 반환한다")
  void delete_returnNotFound_whenPlaylistContentDoesNotExist() throws Exception {
    // given
    doThrow(playlistContentNotFoundException())
        .when(playlistContentService)
        .delete(playlistId, contentId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(delete(CONTENT_URL, playlistId, contentId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.code")
                .value(PlaylistContentErrorCode.PLAYLIST_CONTENT_NOT_FOUND.getCode()));

    verify(playlistContentService).delete(playlistId, contentId, CURRENT_USER_ID);
  }

  private PlaylistException playlistNotFoundException() {
    return new PlaylistException(
        PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId));
  }

  private ContentException contentNotFoundException() {
    return new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
  }

  private PlaylistContentException unauthorizedException() {
    return new PlaylistContentException(
        PlaylistContentErrorCode.UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS,
        Map.of("playlistId", playlistId, "userId", CURRENT_USER_ID));
  }

  private PlaylistContentException alreadyExistsException() {
    return new PlaylistContentException(
        PlaylistContentErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS,
        Map.of("playlistId", playlistId, "contentId", contentId));
  }

  private PlaylistContentException playlistContentNotFoundException() {
    return new PlaylistContentException(
        PlaylistContentErrorCode.PLAYLIST_CONTENT_NOT_FOUND,
        Map.of("playlistId", playlistId, "contentId", contentId));
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
