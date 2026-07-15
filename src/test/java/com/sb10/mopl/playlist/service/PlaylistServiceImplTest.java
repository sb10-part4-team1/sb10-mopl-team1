package com.sb10.mopl.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.dto.PlaylistOwnerDto;
import com.sb10.mopl.playlist.dto.PlaylistUpdateRequest;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlist.mapper.PlaylistMapper;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.playlistsubscription.repository.PlaylistSubscriptionRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceImplTest {

  @Mock private PlaylistRepository playlistRepository;

  @Mock private UserRepository userRepository;

  @Mock private PlaylistMapper playlistMapper;

  @Mock private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @InjectMocks private PlaylistServiceImpl playlistService;

  private UUID playlistId;
  private UUID ownerId;
  private UUID currentUserId;
  private UUID otherUserId;
  private UUID subscriberId;

  private User owner;

  private Playlist playlist;

  private PlaylistCreateRequest createRequest;
  private PlaylistUpdateRequest updateRequest;
  private PlaylistDto playlistDto;
  private PlaylistDto subscribedPlaylistDto;

  @BeforeEach
  void setUp() {
    playlistId = UUID.randomUUID();
    ownerId = UUID.randomUUID();
    currentUserId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    subscriberId = UUID.randomUUID();

    owner = createUser(ownerId);

    createRequest = new PlaylistCreateRequest("플레이리스트 제목", "플레이리스트 설명");
    updateRequest = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

    playlist = createPlaylist(playlistId, owner, "플레이리스트 제목", "플레이리스트 설명");
    playlistDto = createPlaylistDto(playlist, 0L, false);
    subscribedPlaylistDto = createPlaylistDto(playlist, 1L, true);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 성공")
  void create_success() {
    // given
    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(playlistMapper.toEntity(owner, createRequest)).willReturn(playlist);
    given(playlistRepository.save(playlist)).willReturn(playlist);
    given(playlistSubscriptionRepository.countByPlaylistId(playlistId)).willReturn(0L);
    given(playlistMapper.toDto(playlist, 0L, false)).willReturn(playlistDto);

    // when
    PlaylistDto result = playlistService.create(createRequest, ownerId);

    // then
    assertThat(result).isEqualTo(playlistDto);
    verify(playlistRepository).save(playlist);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 소유자가 존재하지 않음")
  void create_fail_ownerNotFound() {
    // given
    given(userRepository.findById(ownerId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.create(createRequest, ownerId))
        .isInstanceOf(UserException.class);

    verify(playlistRepository, never()).save(any());
  }

  @Test
  @DisplayName("플레이리스트 - 단건 조회 성공")
  void findById_success() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.countByPlaylistId(playlistId)).willReturn(1L);
    given(
            playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(
                currentUserId, playlistId))
        .willReturn(true);
    given(playlistMapper.toDto(playlist, 1L, true)).willReturn(subscribedPlaylistDto);

    // when
    PlaylistDto result = playlistService.findById(playlistId, currentUserId);

    // then
    assertThat(result).isEqualTo(subscribedPlaylistDto);
  }

  @Test
  @DisplayName("플레이리스트 - 단건 조회 실패 - 플레이리스트가 존재하지 않음")
  void findById_fail_playlistNotFound() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.findById(playlistId, currentUserId))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 성공")
  void update_success() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.countByPlaylistId(playlistId)).willReturn(0L);
    given(playlistMapper.toDto(playlist, 0L, false)).willReturn(playlistDto);

    // when
    PlaylistDto result = playlistService.update(playlistId, updateRequest, ownerId);

    // then
    assertThat(result).isEqualTo(playlistDto);
    assertThat(playlist.getTitle()).isEqualTo(updateRequest.title());
    assertThat(playlist.getDescription()).isEqualTo(updateRequest.description());
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 플레이리스트가 존재하지 않음")
  void update_fail_playlistNotFound() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.update(playlistId, updateRequest, ownerId))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 소유자가 아님")
  void update_fail_unauthorizedOwner() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.update(playlistId, updateRequest, otherUserId))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS);
  }

  @Test
  @DisplayName("플레이리스트 - 삭제 성공")
  void delete_success() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.of(playlist));

    // when
    playlistService.delete(playlistId, ownerId);

    // then
    verify(playlistRepository).delete(playlist);
  }

  @Test
  @DisplayName("플레이리스트 - 삭제 실패 - 플레이리스트가 존재하지 않음")
  void delete_fail_playlistNotFound() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.delete(playlistId, ownerId))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND);

    verify(playlistRepository, never()).delete(any());
  }

  @Test
  @DisplayName("플레이리스트 - 삭제 실패 - 소유자가 아님")
  void delete_fail_unauthorizedOwner() {
    // given
    given(playlistRepository.findByIdWithOwner(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.delete(playlistId, otherUserId))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS);

    verify(playlistRepository, never()).delete(any());
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 성공")
  void findAll_success() {
    // given
    given(
            playlistRepository.findAllByCondition(
                isNull(),
                eq(ownerId),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("updatedAt"),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)))
        .willReturn(List.of(playlist));

    given(playlistSubscriptionRepository.countByPlaylistIds(List.of(playlistId)))
        .willReturn(List.of(countProjection(playlistId, 0L)));

    given(
            playlistSubscriptionRepository.findSubscribedPlaylistIds(
                currentUserId, List.of(playlistId)))
        .willReturn(Set.of());

    given(playlistMapper.toDto(playlist, 0L, false)).willReturn(playlistDto);
    given(playlistRepository.countByCondition(null, ownerId, null)).willReturn(1L);

    // when
    CursorPageResponse<PlaylistDto> result =
        playlistService.findAll(
            null,
            ownerId,
            null,
            currentUserId,
            null,
            null,
            10,
            "updatedAt",
            SortDirection.DESCENDING);

    // then
    assertThat(result.data()).containsExactly(playlistDto);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 성공 - 다음 페이지가 있음")
  void findAll_success_hasNext() {
    // given
    Playlist nextPlaylist = createPlaylist(UUID.randomUUID(), owner, "다음 플레이리스트", "다음 설명");

    given(
            playlistRepository.findAllByCondition(
                isNull(),
                eq(ownerId),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("updatedAt"),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)))
        .willReturn(List.of(playlist, nextPlaylist));

    given(playlistSubscriptionRepository.countByPlaylistIds(List.of(playlistId)))
        .willReturn(List.of(countProjection(playlistId, 0L)));

    given(
            playlistSubscriptionRepository.findSubscribedPlaylistIds(
                currentUserId, List.of(playlistId)))
        .willReturn(Set.of());

    given(playlistMapper.toDto(playlist, 0L, false)).willReturn(playlistDto);
    given(playlistRepository.countByCondition(null, ownerId, null)).willReturn(2L);

    // when
    CursorPageResponse<PlaylistDto> result =
        playlistService.findAll(
            null,
            ownerId,
            null,
            currentUserId,
            null,
            null,
            1,
            "updatedAt",
            SortDirection.DESCENDING);

    // then
    assertThat(result.data()).containsExactly(playlistDto);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(playlist.getUpdatedAt().toString());
    assertThat(result.nextIdAfter()).isEqualTo(playlist.getId());
    assertThat(result.totalCount()).isEqualTo(2L);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 성공 - 구독자 기준 조회")
  void findAll_success_subscriberIdEqual() {
    // given
    given(
            playlistRepository.findAllByCondition(
                isNull(),
                eq(ownerId),
                eq(subscriberId),
                isNull(),
                isNull(),
                isNull(),
                eq("updatedAt"),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)))
        .willReturn(List.of(playlist));

    given(playlistSubscriptionRepository.countByPlaylistIds(List.of(playlistId)))
        .willReturn(List.of(countProjection(playlistId, 1L)));

    given(
            playlistSubscriptionRepository.findSubscribedPlaylistIds(
                currentUserId, List.of(playlistId)))
        .willReturn(Set.of(playlistId));

    given(playlistMapper.toDto(playlist, 1L, true)).willReturn(subscribedPlaylistDto);
    given(playlistRepository.countByCondition(null, ownerId, subscriberId)).willReturn(1L);

    // when
    CursorPageResponse<PlaylistDto> result =
        playlistService.findAll(
            null,
            ownerId,
            subscriberId,
            currentUserId,
            null,
            null,
            10,
            "updatedAt",
            SortDirection.DESCENDING);

    // then
    assertThat(result.data()).containsExactly(subscribedPlaylistDto);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 성공 - 구독자 수 기준 정렬")
  void findAll_success_sortBySubscriberCount() {
    // given
    given(
            playlistRepository.findAllByCondition(
                isNull(),
                eq(ownerId),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("subscriberCount"),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)))
        .willReturn(List.of(playlist));

    given(playlistSubscriptionRepository.countByPlaylistIds(List.of(playlistId)))
        .willReturn(List.of(countProjection(playlistId, 1L)));

    given(
            playlistSubscriptionRepository.findSubscribedPlaylistIds(
                currentUserId, List.of(playlistId)))
        .willReturn(Set.of(playlistId));

    given(playlistMapper.toDto(playlist, 1L, true)).willReturn(subscribedPlaylistDto);
    given(playlistRepository.countByCondition(null, ownerId, null)).willReturn(1L);

    // when
    CursorPageResponse<PlaylistDto> result =
        playlistService.findAll(
            null,
            ownerId,
            null,
            currentUserId,
            null,
            null,
            10,
            "subscriberCount",
            SortDirection.DESCENDING);

    // then
    assertThat(result.data()).containsExactly(subscribedPlaylistDto);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 성공 - updatedAt 오름차순")
  void findAll_success_updatedAtAscending() {
    // given
    given(
            playlistRepository.findAllByCondition(
                isNull(),
                eq(ownerId),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("updatedAt"),
                eq(SortDirection.ASCENDING),
                any(Pageable.class)))
        .willReturn(List.of(playlist));

    given(playlistSubscriptionRepository.countByPlaylistIds(List.of(playlistId)))
        .willReturn(List.of(countProjection(playlistId, 0L)));

    given(
            playlistSubscriptionRepository.findSubscribedPlaylistIds(
                currentUserId, List.of(playlistId)))
        .willReturn(Set.of());

    given(playlistMapper.toDto(playlist, 0L, false)).willReturn(playlistDto);
    given(playlistRepository.countByCondition(null, ownerId, null)).willReturn(1L);

    // when
    CursorPageResponse<PlaylistDto> result =
        playlistService.findAll(
            null,
            ownerId,
            null,
            currentUserId,
            null,
            null,
            10,
            "updatedAt",
            SortDirection.ASCENDING);

    // then
    assertThat(result.data()).containsExactly(playlistDto);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 성공 - 구독자 수 오름차순")
  void findAll_success_subscriberCountAscending() {
    // given
    given(
            playlistRepository.findAllByCondition(
                isNull(),
                eq(ownerId),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("subscriberCount"),
                eq(SortDirection.ASCENDING),
                any(Pageable.class)))
        .willReturn(List.of(playlist));

    given(playlistSubscriptionRepository.countByPlaylistIds(List.of(playlistId)))
        .willReturn(List.of(countProjection(playlistId, 1L)));

    given(
            playlistSubscriptionRepository.findSubscribedPlaylistIds(
                currentUserId, List.of(playlistId)))
        .willReturn(Set.of(playlistId));

    given(playlistMapper.toDto(playlist, 1L, true)).willReturn(subscribedPlaylistDto);
    given(playlistRepository.countByCondition(null, ownerId, null)).willReturn(1L);

    // when
    CursorPageResponse<PlaylistDto> result =
        playlistService.findAll(
            null,
            ownerId,
            null,
            currentUserId,
            null,
            null,
            10,
            "subscriberCount",
            SortDirection.ASCENDING);

    // then
    assertThat(result.data()).containsExactly(subscribedPlaylistDto);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - cursor와 idAfter 중 하나만 전달")
  void findAll_fail_cursorAndIdAfterMismatch() {
    // given
    String cursor = Instant.now().toString();

    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    cursor,
                    null,
                    10,
                    "updatedAt",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - 지원하지 않는 정렬 기준")
  void findAll_fail_invalidSortBy() {
    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    null,
                    null,
                    10,
                    "createdAt",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - 정렬 방향이 null")
  void findAll_fail_sortDirectionNull() {
    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null, ownerId, null, currentUserId, null, null, 10, "updatedAt", null))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - limit이 1보다 작음")
  void findAll_fail_invalidLimitLessThanOne() {
    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    null,
                    null,
                    0,
                    "updatedAt",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - limit이 최대값을 초과함")
  void findAll_fail_invalidLimitGreaterThanMax() {
    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    null,
                    null,
                    101,
                    "updatedAt",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - cursor 형식이 올바르지 않음")
  void findAll_fail_invalidCursorFormat() {
    // given
    UUID idAfter = UUID.randomUUID();

    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    "invalid-cursor",
                    idAfter,
                    10,
                    "updatedAt",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - 구독자 수 커서 형식이 올바르지 않음")
  void findAll_fail_invalidSubscriberCountCursorFormat() {
    // given
    UUID idAfter = UUID.randomUUID();

    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    "invalid-cursor",
                    idAfter,
                    10,
                    "subscriberCount",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 목록 조회 실패 - 구독자 수 커서가 음수")
  void findAll_fail_negativeSubscriberCountCursor() {
    // given
    UUID idAfter = UUID.randomUUID();

    // when & then
    assertThatThrownBy(
            () ->
                playlistService.findAll(
                    null,
                    ownerId,
                    null,
                    currentUserId,
                    "-1",
                    idAfter,
                    10,
                    "subscriberCount",
                    SortDirection.DESCENDING))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  // 테스트용 User 생성 후 id 주입
  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }

  // 테스트용 Playlist 생성 후 id, updatedAt 주입
  private Playlist createPlaylist(UUID playlistId, User owner, String title, String description) {

    Playlist playlist = new Playlist(owner, title, description);
    ReflectionTestUtils.setField(playlist, "id", playlistId);
    ReflectionTestUtils.setField(playlist, "updatedAt", Instant.now());

    return playlist;
  }

  // 테스트 검증에 사용할 PlaylistDto 생성
  private PlaylistDto createPlaylistDto(
      Playlist playlist, long subscriberCount, boolean subscribedByMe) {

    PlaylistOwnerDto ownerDto = new PlaylistOwnerDto(playlist.getOwner().getId(), "테스트유저", null);

    return new PlaylistDto(
        playlist.getId(),
        ownerDto,
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getUpdatedAt(),
        subscriberCount,
        subscribedByMe,
        List.of());
  }

  private PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection countProjection(
      UUID playlistId, Long subscriberCount) {

    return new PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection() {

      @Override
      public UUID getPlaylistId() {
        return playlistId;
      }

      @Override
      public Long getSubscriberCount() {
        return subscriberCount;
      }
    };
  }
}
