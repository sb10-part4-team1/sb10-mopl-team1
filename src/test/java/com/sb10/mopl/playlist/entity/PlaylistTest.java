package com.sb10.mopl.playlist.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaylistTest {

  private User owner;

  @BeforeEach
  void setUp() {
    owner = createUser(UUID.randomUUID());
  }

  @Test
  @DisplayName("플레이리스트 - 생성 성공")
  void create_success() {
    // when
    Playlist playlist = new Playlist(owner, "플레이리스트 제목", "플레이리스트 설명");

    // then
    assertThat(playlist.getOwner()).isEqualTo(owner);
    assertThat(playlist.getTitle()).isEqualTo("플레이리스트 제목");
    assertThat(playlist.getDescription()).isEqualTo("플레이리스트 설명");
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 소유자가 없음")
  void create_fail_ownerNull() {
    // when & then
    assertThatThrownBy(() -> new Playlist(null, "플레이리스트 제목", "플레이리스트 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 제목이 null")
  void create_fail_titleNull() {
    // when & then
    assertThatThrownBy(() -> new Playlist(owner, null, "플레이리스트 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 제목이 비어 있음")
  void create_fail_titleBlank() {
    // when & then
    assertThatThrownBy(() -> new Playlist(owner, " ", "플레이리스트 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 제목이 255자를 초과함")
  void create_fail_titleTooLong() {
    // given
    String title = "가".repeat(256);

    // when & then
    assertThatThrownBy(() -> new Playlist(owner, title, "플레이리스트 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 설명이 null")
  void create_fail_descriptionNull() {
    // when & then
    assertThatThrownBy(() -> new Playlist(owner, "플레이리스트 제목", null))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 생성 실패 - 설명이 비어 있음")
  void create_fail_descriptionBlank() {
    // when & then
    assertThatThrownBy(() -> new Playlist(owner, "플레이리스트 제목", " "))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 성공")
  void update_success() {
    // given
    Playlist playlist = new Playlist(owner, "기존 제목", "기존 설명");

    // when
    playlist.update("수정된 제목", "수정된 설명");

    // then
    assertThat(playlist.getTitle()).isEqualTo("수정된 제목");
    assertThat(playlist.getDescription()).isEqualTo("수정된 설명");
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 제목이 null")
  void update_fail_titleNull() {
    // given
    Playlist playlist = new Playlist(owner, "기존 제목", "기존 설명");

    // when & then
    assertThatThrownBy(() -> playlist.update(null, "수정된 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 제목이 비어 있음")
  void update_fail_titleBlank() {
    // given
    Playlist playlist = new Playlist(owner, "기존 제목", "기존 설명");

    // when & then
    assertThatThrownBy(() -> playlist.update(" ", "수정된 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 제목이 255자를 초과함")
  void update_fail_titleTooLong() {
    // given
    Playlist playlist = new Playlist(owner, "기존 제목", "기존 설명");
    String title = "가".repeat(256);

    // when & then
    assertThatThrownBy(() -> playlist.update(title, "수정된 설명"))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 설명이 null")
  void update_fail_descriptionNull() {
    // given
    Playlist playlist = new Playlist(owner, "기존 제목", "기존 설명");

    // when & then
    assertThatThrownBy(() -> playlist.update("수정된 제목", null))
        .isInstanceOf(PlaylistException.class)
        .extracting("errorCode")
        .isEqualTo(PlaylistErrorCode.INVALID_PLAYLIST_VALUE);
  }

  @Test
  @DisplayName("플레이리스트 - 수정 실패 - 설명이 비어 있음")
  void update_fail_descriptionBlank() {
    // given
    Playlist playlist = new Playlist(owner, "기존 제목", "기존 설명");

    // when & then
    assertThatThrownBy(() -> playlist.update("수정된 제목", " "))
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
}
