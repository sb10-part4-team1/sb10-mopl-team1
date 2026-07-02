package com.sb10.mopl.playlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.common.pagination.SortDirectionConverter;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.dto.PlaylistOwnerDto;
import com.sb10.mopl.playlist.dto.PlaylistUpdateRequest;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlist.service.PlaylistService;
import jakarta.annotation.Nullable;
import java.time.Instant;
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
    controllers = PlaylistController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import({
  GlobalExceptionHandler.class,
  SortDirectionConverter.class,
  PlaylistControllerTest.TestCurrentUserConfig.class
})
class PlaylistControllerTest {

  private static final String BASE_URL = "/api/playlists";
  private static final String DETAIL_URL = "/api/playlists/{playlistId}";

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PlaylistService playlistService;

  private UUID playlistId;
  private UUID ownerId;

  private PlaylistCreateRequest createRequest;
  private PlaylistUpdateRequest updateRequest;
  private PlaylistDto playlistDto;

  @BeforeEach
  void setUp() {
    playlistId = UUID.randomUUID();
    ownerId = CURRENT_USER_ID;

    createRequest = new PlaylistCreateRequest("플레이리스트 제목", "플레이리스트 설명");
    updateRequest = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

    playlistDto = createPlaylistDto(playlistId, ownerId, "플레이리스트 제목", "플레이리스트 설명");
  }

  @Test
  @DisplayName("플레이리스트 생성 요청이 유효하면 201 Created와 PlaylistDto를 반환한다")
  void create_returnCreated_whenRequestIsValid() throws Exception {
    // given
    when(playlistService.create(any(PlaylistCreateRequest.class), eq(ownerId)))
        .thenReturn(playlistDto);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(toJson(createRequest)));

    // then
    resultActions
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(playlistId.toString()))
        .andExpect(jsonPath("$.title").value("플레이리스트 제목"))
        .andExpect(jsonPath("$.description").value("플레이리스트 설명"));

    verify(playlistService).create(any(PlaylistCreateRequest.class), eq(ownerId));
  }

  @Test
  @DisplayName("플레이리스트 생성 요청 시 제목이 비어있으면 400 Bad Request를 반환한다")
  void create_returnBadRequest_whenTitleIsBlank() throws Exception {
    // given
    PlaylistCreateRequest request = new PlaylistCreateRequest(" ", "플레이리스트 설명");

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

    // then
    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("플레이리스트 생성 요청 시 설명이 비어있으면 400 Bad Request를 반환한다")
  void create_returnBadRequest_whenDescriptionIsBlank() throws Exception {
    // given
    PlaylistCreateRequest request = new PlaylistCreateRequest("플레이리스트 제목", " ");

    // when
    ResultActions resultActions =
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

    // then
    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("유효한 파라미터로 플레이리스트 목록 조회 요청 시 200 OK와 CursorPageResponse를 반환한다")
  void findAll_returnOk_whenParamsAreValid() throws Exception {
    // given
    CursorPageResponse<PlaylistDto> response =
        new CursorPageResponse<>(
            List.of(playlistDto), null, null, false, 1L, "updatedAt", SortDirection.DESCENDING);

    when(playlistService.findAll(
            eq(null),
            eq(ownerId),
            eq(null),
            eq(null),
            eq(null),
            eq(10),
            eq("updatedAt"),
            eq(SortDirection.DESCENDING)))
        .thenReturn(response);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            get(BASE_URL)
                .param("ownerIdEqual", ownerId.toString())
                .param("limit", "10")
                .param("sortBy", "updatedAt")
                .param("sortDirection", "DESCENDING"));

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(playlistId.toString()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("플레이리스트 목록 조회 요청 시 limit이 누락되면 400 Bad Request를 반환한다")
  void findAll_returnBadRequest_whenLimitIsMissing() throws Exception {
    // when
    ResultActions resultActions =
        mockMvc.perform(
            get(BASE_URL).param("sortBy", "updatedAt").param("sortDirection", "DESCENDING"));

    // then
    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("플레이리스트 목록 조회 요청 시 정렬 기준(sortBy)이 누락되면 400 Bad Request를 반환한다")
  void findAll_returnBadRequest_whenSortByIsMissing() throws Exception {
    // when
    ResultActions resultActions =
        mockMvc.perform(get(BASE_URL).param("limit", "10").param("sortDirection", "DESCENDING"));

    // then
    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("플레이리스트 목록 조회 요청 시 정렬 방향(sortDirection)이 잘못되면 400 Bad Request를 반환한다")
  void findAll_returnBadRequest_whenSortDirectionIsInvalid() throws Exception {
    // when
    ResultActions resultActions =
        mockMvc.perform(
            get(BASE_URL)
                .param("limit", "10")
                .param("sortBy", "updatedAt")
                .param("sortDirection", "DOWN"));

    // then
    resultActions
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"))
        .andExpect(
            jsonPath("$.details.sortDirection")
                .value("지원하지 않는 값입니다. (선택 가능한 값: [ASCENDING, DESCENDING])"));
  }

  @Test
  @DisplayName("존재하는 플레이리스트 단건 조회 요청 시 200 OK와 PlaylistDto를 반환한다")
  void findById_returnOk_whenPlaylistExists() throws Exception {
    // given
    when(playlistService.findById(playlistId)).thenReturn(playlistDto);

    // when
    ResultActions resultActions = mockMvc.perform(get(DETAIL_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(playlistId.toString()))
        .andExpect(jsonPath("$.title").value("플레이리스트 제목"));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트 단건 조회 요청 시 404 Not Found를 반환한다")
  void findById_returnNotFound_whenPlaylistDoesNotExist() throws Exception {
    // given
    when(playlistService.findById(playlistId)).thenThrow(playlistNotFoundException());

    // when
    ResultActions resultActions = mockMvc.perform(get(DETAIL_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getCode()));
  }

  @Test
  @DisplayName("플레이리스트 수정 요청이 유효하면 200 OK와 PlaylistDto를 반환한다")
  void update_returnOk_whenRequestIsValid() throws Exception {
    // given
    PlaylistDto updatedResponse = createPlaylistDto(playlistId, ownerId, "수정된 제목", "수정된 설명");

    when(playlistService.update(eq(playlistId), any(PlaylistUpdateRequest.class), eq(ownerId)))
        .thenReturn(updatedResponse);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(DETAIL_URL, playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest)));

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(playlistId.toString()))
        .andExpect(jsonPath("$.title").value("수정된 제목"))
        .andExpect(jsonPath("$.description").value("수정된 설명"));

    verify(playlistService).update(eq(playlistId), any(PlaylistUpdateRequest.class), eq(ownerId));
  }

  @Test
  @DisplayName("플레이리스트 수정 요청 시 제목이 비어있으면 400 Bad Request를 반환한다")
  void update_returnBadRequest_whenTitleIsBlank() throws Exception {
    // given
    PlaylistUpdateRequest request = new PlaylistUpdateRequest(" ", "수정된 설명");

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(DETAIL_URL, playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

    // then
    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("플레이리스트 수정 요청 시 설명이 비어있으면 400 Bad Request를 반환한다")
  void update_returnBadRequest_whenDescriptionIsBlank() throws Exception {
    // given
    PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", " ");

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(DETAIL_URL, playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

    // then
    expectBadRequestWithSystemInputError(resultActions);
  }

  @Test
  @DisplayName("플레이리스트 삭제 요청이 유효하면 204 No Content를 반환한다")
  void delete_returnNoContent_whenPlaylistExists() throws Exception {
    // when
    ResultActions resultActions = mockMvc.perform(delete(DETAIL_URL, playlistId));

    // then
    resultActions.andExpect(status().isNoContent());

    verify(playlistService).delete(playlistId, ownerId);
  }

  @Test
  @DisplayName("플레이리스트 수정 실패 - 소유자가 아니면 403을 반환한다")
  void updateFailUnauthorizedOwner() throws Exception {
    // given
    given(
            playlistService.update(
                eq(playlistId), any(PlaylistUpdateRequest.class), eq(CURRENT_USER_ID)))
        .willThrow(
            new PlaylistException(
                PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS,
                Map.of("playlistId", playlistId, "userId", CURRENT_USER_ID)));

    // when
    ResultActions resultActions =
        mockMvc.perform(
            patch(DETAIL_URL, playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest)));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code").value(PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS.getCode()));

    verify(playlistService)
        .update(eq(playlistId), any(PlaylistUpdateRequest.class), eq(CURRENT_USER_ID));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트 삭제 요청 시 404 Not Found를 반환한다")
  void delete_returnNotFound_whenPlaylistDoesNotExist() throws Exception {
    // given
    doThrow(playlistNotFoundException()).when(playlistService).delete(playlistId, ownerId);

    // when
    ResultActions resultActions = mockMvc.perform(delete(DETAIL_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getCode()));
  }

  @Test
  @DisplayName("플레이리스트 삭제 실패 - 소유자가 아니면 403을 반환한다")
  void deleteFailUnauthorizedOwner() throws Exception {
    // given
    willThrow(
            new PlaylistException(
                PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS,
                Map.of("playlistId", playlistId, "userId", CURRENT_USER_ID)))
        .given(playlistService)
        .delete(playlistId, CURRENT_USER_ID);

    // when
    ResultActions resultActions = mockMvc.perform(delete(DETAIL_URL, playlistId));

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code").value(PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS.getCode()));

    verify(playlistService).delete(playlistId, CURRENT_USER_ID);
  }

  private String toJson(Object request) throws Exception {
    return objectMapper.writeValueAsString(request);
  }

  private void expectBadRequestWithSystemInputError(ResultActions resultActions) throws Exception {
    resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SYS01"));
  }

  private PlaylistException playlistNotFoundException() {
    return new PlaylistException(
        PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId));
  }

  private PlaylistDto createPlaylistDto(
      UUID playlistId, UUID ownerId, String title, String description) {
    PlaylistOwnerDto ownerDto = new PlaylistOwnerDto(ownerId, "테스트유저", null);

    return new PlaylistDto(
        playlistId, ownerDto, title, description, Instant.now(), 0L, false, List.of());
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
