package com.sb10.mopl.playlistcontent.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.playlistcontent.entity.PlaylistContent;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class PlaylistContentRepositoryTest {

  @Autowired private PlaylistContentRepository playlistContentRepository;

  @Autowired private PlaylistRepository playlistRepository;

  @Autowired private ContentRepository contentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private EntityManager entityManager;

  private Playlist playlist;
  private Playlist otherPlaylist;

  private Content content;
  private Content otherContent;

  @BeforeEach
  void setUp() {
    User owner = userRepository.save(createUser("소유자", "owner@example.com"));

    playlist = playlistRepository.save(new Playlist(owner, "플레이리스트 제목", "플레이리스트 설명"));

    otherPlaylist = playlistRepository.save(new Playlist(owner, "다른 플레이리스트 제목", "다른 플레이리스트 설명"));

    content =
        contentRepository.save(
            Content.create("콘텐츠 제목", ContentType.MOVIE, "콘텐츠 설명", "/uploads/content.jpg"));

    otherContent =
        contentRepository.save(
            Content.create(
                "다른 콘텐츠 제목", ContentType.TV_SERIES, "다른 콘텐츠 설명", "/uploads/other-content.jpg"));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("플레이리스트와 콘텐츠의 매핑이 존재하면 true를 반환한다")
  void existsByPlaylistIdAndContentId_returnTrue_whenPlaylistContentExists() {
    // given
    playlistContentRepository.save(new PlaylistContent(playlist, content));

    entityManager.flush();
    entityManager.clear();

    // when
    boolean result =
        playlistContentRepository.existsByPlaylistIdAndContentId(playlist.getId(), content.getId());

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("플레이리스트와 콘텐츠 조합이 일치하지 않으면 false를 반환한다")
  void existsByPlaylistIdAndContentId_returnFalse_whenCombinationDoesNotMatch() {
    // given
    playlistContentRepository.saveAll(
        List.of(
            new PlaylistContent(playlist, otherContent),
            new PlaylistContent(otherPlaylist, content)));

    entityManager.flush();
    entityManager.clear();

    // when
    boolean result =
        playlistContentRepository.existsByPlaylistIdAndContentId(playlist.getId(), content.getId());

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("플레이리스트 ID와 콘텐츠 ID로 매핑 정보를 조회한다")
  void findByPlaylistIdAndContentId_returnPlaylistContent_whenExists() {
    // given
    final PlaylistContent savedPlaylistContent =
        playlistContentRepository.save(new PlaylistContent(playlist, content));

    entityManager.flush();
    entityManager.clear();

    // when
    Optional<PlaylistContent> result =
        playlistContentRepository.findByPlaylistIdAndContentId(playlist.getId(), content.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedPlaylistContent.getId());
    assertThat(result.get().getPlaylist().getId()).isEqualTo(playlist.getId());
    assertThat(result.get().getContent().getId()).isEqualTo(content.getId());
  }

  @Test
  @DisplayName("플레이리스트와 콘텐츠 조합이 일치하지 않으면 빈 Optional을 반환한다")
  void findByPlaylistIdAndContentId_returnEmpty_whenCombinationDoesNotMatch() {
    // given
    playlistContentRepository.saveAll(
        List.of(
            new PlaylistContent(playlist, otherContent),
            new PlaylistContent(otherPlaylist, content)));

    entityManager.flush();
    entityManager.clear();

    // when
    Optional<PlaylistContent> result =
        playlistContentRepository.findByPlaylistIdAndContentId(playlist.getId(), content.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("플레이리스트 ID 목록에 속한 콘텐츠를 함께 조회한다")
  void findAllWithContentByPlaylistIds_returnPlaylistContents_whenExists() {
    // given
    final PlaylistContent firstPlaylistContent =
        playlistContentRepository.save(new PlaylistContent(playlist, content));

    final PlaylistContent secondPlaylistContent =
        playlistContentRepository.save(new PlaylistContent(playlist, otherContent));

    playlistContentRepository.save(new PlaylistContent(otherPlaylist, content));

    entityManager.flush();
    entityManager.clear();

    // when
    List<PlaylistContent> result =
        playlistContentRepository.findAllWithContentByPlaylistIds(List.of(playlist.getId()));

    // then
    assertThat(result).hasSize(2);

    assertThat(result)
        .extracting(PlaylistContent::getId)
        .containsExactlyInAnyOrder(firstPlaylistContent.getId(), secondPlaylistContent.getId());

    assertThat(result)
        .allSatisfy(
            playlistContent ->
                assertThat(playlistContent.getPlaylist().getId()).isEqualTo(playlist.getId()));

    assertThat(result)
        .extracting(playlistContent -> playlistContent.getContent().getId())
        .containsExactlyInAnyOrder(content.getId(), otherContent.getId());

    assertThat(result)
        .extracting(playlistContent -> playlistContent.getContent().getTitle())
        .containsExactlyInAnyOrder(content.getTitle(), otherContent.getTitle());
  }

  @Test
  @DisplayName("여러 플레이리스트 ID에 속한 콘텐츠를 함께 조회한다")
  void findAllWithContentByPlaylistIds_returnContentsForMultiplePlaylists() {
    // given
    PlaylistContent playlistContent =
        playlistContentRepository.save(new PlaylistContent(playlist, content));

    PlaylistContent otherPlaylistContent =
        playlistContentRepository.save(new PlaylistContent(otherPlaylist, otherContent));

    entityManager.flush();
    entityManager.clear();

    // when
    List<PlaylistContent> result =
        playlistContentRepository.findAllWithContentByPlaylistIds(
            List.of(playlist.getId(), otherPlaylist.getId()));

    // then
    assertThat(result)
        .extracting(PlaylistContent::getId)
        .containsExactlyInAnyOrder(playlistContent.getId(), otherPlaylistContent.getId());

    assertThat(result)
        .extracting(resultItem -> resultItem.getPlaylist().getId())
        .containsExactlyInAnyOrder(playlist.getId(), otherPlaylist.getId());

    assertThat(result)
        .extracting(resultItem -> resultItem.getContent().getId())
        .containsExactlyInAnyOrder(content.getId(), otherContent.getId());
  }

  @Test
  @DisplayName("조회 대상에 포함되지 않은 플레이리스트의 콘텐츠는 반환하지 않는다")
  void findAllWithContentByPlaylistIds_excludeUnrequestedPlaylistContents() {
    // given
    playlistContentRepository.save(new PlaylistContent(playlist, content));

    playlistContentRepository.save(new PlaylistContent(otherPlaylist, otherContent));

    entityManager.flush();
    entityManager.clear();

    // when
    List<PlaylistContent> result =
        playlistContentRepository.findAllWithContentByPlaylistIds(List.of(playlist.getId()));

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlaylist().getId()).isEqualTo(playlist.getId());
    assertThat(result.get(0).getContent().getId()).isEqualTo(content.getId());

    assertThat(result)
        .noneMatch(resultItem -> resultItem.getPlaylist().getId().equals(otherPlaylist.getId()));
  }

  @Test
  @DisplayName("플레이리스트 ID 목록이 비어 있으면 빈 목록을 반환한다")
  void findAllWithContentByPlaylistIds_returnEmpty_whenPlaylistIdsEmpty() {
    // when
    List<PlaylistContent> result =
        playlistContentRepository.findAllWithContentByPlaylistIds(List.of());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("플레이리스트 ID 목록이 null이면 빈 목록을 반환한다")
  void findAllWithContentByPlaylistIds_returnEmpty_whenPlaylistIdsNull() {
    // when
    List<PlaylistContent> result = playlistContentRepository.findAllWithContentByPlaylistIds(null);

    // then
    assertThat(result).isEmpty();
  }

  // 테스트용 사용자 생성
  private User createUser(String name, String email) {
    return User.createUser(name, email, "password", null);
  }
}
