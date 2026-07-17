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
                "다른 콘텐츠 제목", ContentType.MOVIE, "다른 콘텐츠 설명", "/uploads/other-content.jpg"));

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

  // 테스트용 사용자 생성
  private User createUser(String name, String email) {
    return User.createUser(name, email, "password", null);
  }
}
