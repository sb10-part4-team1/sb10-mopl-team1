package com.sb10.mopl.playlistsubscription.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class PlaylistSubscriptionRepositoryTest {

  @Autowired private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private PlaylistRepository playlistRepository;

  @Autowired private EntityManager entityManager;

  private User subscriber;
  private User otherSubscriber;

  private Playlist playlist;
  private Playlist otherPlaylist;

  @BeforeEach
  void setUp() {
    User owner = userRepository.save(createUser("소유자", "owner@example.com"));

    subscriber = userRepository.save(createUser("구독자", "subscriber@example.com"));

    otherSubscriber = userRepository.save(createUser("다른 구독자", "other-subscriber@example.com"));

    playlist = playlistRepository.save(new Playlist(owner, "플레이리스트 제목", "플레이리스트 설명"));

    otherPlaylist = playlistRepository.save(new Playlist(owner, "다른 플레이리스트 제목", "다른 플레이리스트 설명"));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 구독 관계 존재 여부 조회 성공")
  void existsBySubscriberIdAndPlaylistId_success() {
    // given
    playlistSubscriptionRepository.save(new PlaylistSubscription(subscriber, playlist));

    entityManager.flush();
    entityManager.clear();

    // when
    boolean result =
        playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(
            subscriber.getId(), playlist.getId());

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 구독 관계가 없으면 false 반환")
  void existsBySubscriberIdAndPlaylistId_notExists() {
    // when
    boolean result =
        playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(
            subscriber.getId(), playlist.getId());

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 사용자와 플레이리스트 기준 구독 조회 성공")
  void findBySubscriberIdAndPlaylistId_success() {
    // given
    final PlaylistSubscription savedSubscription =
        playlistSubscriptionRepository.save(new PlaylistSubscription(subscriber, playlist));

    entityManager.flush();
    entityManager.clear();

    // when
    Optional<PlaylistSubscription> result =
        playlistSubscriptionRepository.findBySubscriberIdAndPlaylistId(
            subscriber.getId(), playlist.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedSubscription.getId());
    assertThat(result.get().getSubscriber().getId()).isEqualTo(subscriber.getId());
    assertThat(result.get().getPlaylist().getId()).isEqualTo(playlist.getId());
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 구독 관계가 없으면 빈 Optional 반환")
  void findBySubscriberIdAndPlaylistId_notFound() {
    // when
    Optional<PlaylistSubscription> result =
        playlistSubscriptionRepository.findBySubscriberIdAndPlaylistId(
            subscriber.getId(), playlist.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("동일한 사용자와 플레이리스트를 중복 구독하면 무결성 예외가 발생한다")
  void save_throwDataIntegrityViolation_whenSubscriptionIsDuplicated() {
    // given
    playlistSubscriptionRepository.save(new PlaylistSubscription(subscriber, playlist));
    playlistSubscriptionRepository.flush();

    entityManager.clear();

    // when & then
    assertThatThrownBy(
            () -> {
              playlistSubscriptionRepository.save(new PlaylistSubscription(subscriber, playlist));
              playlistSubscriptionRepository.flush();
            })
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 특정 플레이리스트 구독자 수 조회 성공")
  void countByPlaylistId_success() {
    // given
    playlistSubscriptionRepository.saveAll(
        List.of(
            new PlaylistSubscription(subscriber, playlist),
            new PlaylistSubscription(otherSubscriber, playlist)));

    entityManager.flush();
    entityManager.clear();

    // when
    long result = playlistSubscriptionRepository.countByPlaylistId(playlist.getId());

    // then
    assertThat(result).isEqualTo(2L);
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 구독자가 없으면 0 반환")
  void countByPlaylistId_noSubscriber() {
    // when
    long result = playlistSubscriptionRepository.countByPlaylistId(playlist.getId());

    // then
    assertThat(result).isZero();
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 플레이리스트별 구독자 수 bulk 조회 성공")
  void countByPlaylistIds_success() {
    // given
    playlistSubscriptionRepository.saveAll(
        List.of(
            new PlaylistSubscription(subscriber, playlist),
            new PlaylistSubscription(otherSubscriber, playlist),
            new PlaylistSubscription(subscriber, otherPlaylist)));

    entityManager.flush();
    entityManager.clear();

    // when
    List<PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection> result =
        playlistSubscriptionRepository.countByPlaylistIds(
            List.of(playlist.getId(), otherPlaylist.getId()));

    // then
    assertThat(result)
        .hasSize(2)
        .anySatisfy(
            projection -> {
              assertThat(projection.getPlaylistId()).isEqualTo(playlist.getId());
              assertThat(projection.getSubscriberCount()).isEqualTo(2L);
            })
        .anySatisfy(
            projection -> {
              assertThat(projection.getPlaylistId()).isEqualTo(otherPlaylist.getId());
              assertThat(projection.getSubscriberCount()).isEqualTo(1L);
            });
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 구독자가 없는 플레이리스트는 bulk 조회 결과에서 제외")
  void countByPlaylistIds_excludesPlaylistWithoutSubscriber() {
    // given
    playlistSubscriptionRepository.save(new PlaylistSubscription(subscriber, playlist));

    entityManager.flush();
    entityManager.clear();

    // when
    List<PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection> result =
        playlistSubscriptionRepository.countByPlaylistIds(
            List.of(playlist.getId(), otherPlaylist.getId()));

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlaylistId()).isEqualTo(playlist.getId());
    assertThat(result.get(0).getSubscriberCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 빈 플레이리스트 ID 목록의 구독자 수 조회")
  void countByPlaylistIds_emptyPlaylistIds() {
    // when
    List<PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection> result =
        playlistSubscriptionRepository.countByPlaylistIds(List.of());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 현재 사용자가 구독한 플레이리스트 ID bulk 조회 성공")
  void findSubscribedPlaylistIds_success() {
    // given
    playlistSubscriptionRepository.save(new PlaylistSubscription(subscriber, playlist));

    playlistSubscriptionRepository.save(new PlaylistSubscription(otherSubscriber, otherPlaylist));

    entityManager.flush();
    entityManager.clear();

    // when
    Set<UUID> result =
        playlistSubscriptionRepository.findSubscribedPlaylistIds(
            subscriber.getId(), List.of(playlist.getId(), otherPlaylist.getId()));

    // then
    assertThat(result).containsExactly(playlist.getId());
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 조회 대상 중 구독한 플레이리스트가 없으면 빈 Set 반환")
  void findSubscribedPlaylistIds_notSubscribed() {
    // when
    Set<UUID> result =
        playlistSubscriptionRepository.findSubscribedPlaylistIds(
            subscriber.getId(), List.of(playlist.getId(), otherPlaylist.getId()));

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("플레이리스트 구독 Repository - 빈 플레이리스트 ID 목록의 구독 여부 조회")
  void findSubscribedPlaylistIds_emptyPlaylistIds() {
    // when
    Set<UUID> result =
        playlistSubscriptionRepository.findSubscribedPlaylistIds(subscriber.getId(), List.of());

    // then
    assertThat(result).isEmpty();
  }

  // 테스트용 사용자 생성
  private User createUser(String name, String email) {
    return User.createUser(name, email, "password", null);
  }
}
