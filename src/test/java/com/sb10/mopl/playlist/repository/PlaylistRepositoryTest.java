package com.sb10.mopl.playlist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import com.sb10.mopl.playlistsubscription.repository.PlaylistSubscriptionRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class PlaylistRepositoryTest {

  private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 10);

  @Autowired
  private PlaylistRepository playlistRepository;

  @Autowired
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  private User owner;
  private User subscriber;
  private User otherSubscriber;

  private Playlist firstPlaylist;
  private Playlist secondPlaylist;
  private Playlist thirdPlaylist;

  @BeforeEach
  void setUp() {
    owner = userRepository.save(createUser("소유자", "owner@example.com"));

    User otherOwner =
      userRepository.save(createUser("다른 소유자", "other-owner@example.com"));

    subscriber = userRepository.save(createUser("구독자", "subscriber@example.com"));
    otherSubscriber =
      userRepository.save(createUser("다른 구독자", "other-subscriber@example.com"));

    firstPlaylist =
      playlistRepository.save(
        new Playlist(owner, "영화 추천 모음", "재미있는 영화 플레이리스트"));

    secondPlaylist =
      playlistRepository.save(
        new Playlist(owner, "음악 추천 모음", "집중할 때 듣는 음악"));

    thirdPlaylist =
      playlistRepository.save(
        new Playlist(otherOwner, "드라마 정주행", "주말에 볼 드라마"));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("조회 조건이 없으면 모든 플레이리스트를 조회한다")
  void findAllByCondition_returnAll_whenConditionIsEmpty() {
    // when
    List<Playlist> result =
      findAllByCondition(
        null,
        null,
        null,
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactlyInAnyOrder(
        firstPlaylist.getId(), secondPlaylist.getId(), thirdPlaylist.getId());
  }

  @Test
  @DisplayName("제목에 검색어가 포함된 플레이리스트를 조회한다")
  void findAllByCondition_filterByTitleKeyword() {
    // when
    List<Playlist> result =
      findAllByCondition(
        "영화",
        null,
        null,
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactly(firstPlaylist.getId());
  }

  @Test
  @DisplayName("설명에 검색어가 포함된 플레이리스트를 공백을 제거하여 조회한다")
  void findAllByCondition_filterByDescriptionKeyword() {
    // when
    List<Playlist> result =
      findAllByCondition(
        "  드라마  ",
        null,
        null,
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactly(thirdPlaylist.getId());
  }

  @Test
  @DisplayName("소유자 ID가 일치하는 플레이리스트만 조회한다")
  void findAllByCondition_filterByOwnerId() {
    // when
    List<Playlist> result =
      findAllByCondition(
        null,
        owner.getId(),
        null,
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactlyInAnyOrder(firstPlaylist.getId(), secondPlaylist.getId());
  }

  @Test
  @DisplayName("사용자가 구독한 플레이리스트만 조회한다")
  void findAllByCondition_filterBySubscriberId() {
    // given
    playlistSubscriptionRepository.saveAll(
      List.of(
        new PlaylistSubscription(subscriber, firstPlaylist),
        new PlaylistSubscription(subscriber, thirdPlaylist),
        new PlaylistSubscription(otherSubscriber, secondPlaylist)));

    entityManager.flush();
    entityManager.clear();

    // when
    List<Playlist> result =
      findAllByCondition(
        null,
        null,
        subscriber.getId(),
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactlyInAnyOrder(firstPlaylist.getId(), thirdPlaylist.getId());
  }

  @Test
  @DisplayName("여러 조회 조건을 모두 만족하는 플레이리스트만 조회한다")
  void findAllByCondition_filterByCombinedConditions() {
    // given
    playlistSubscriptionRepository.saveAll(
      List.of(
        new PlaylistSubscription(subscriber, firstPlaylist),
        new PlaylistSubscription(subscriber, thirdPlaylist)));

    entityManager.flush();
    entityManager.clear();

    // when
    List<Playlist> result =
      findAllByCondition(
        "추천",
        owner.getId(),
        subscriber.getId(),
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactly(firstPlaylist.getId());
  }

  @Test
  @DisplayName("구독자 수를 기준으로 오름차순 조회한다")
  void findAllByCondition_sortBySubscriberCountAscending() {
    // given
    saveSubscriptionsForSubscriberCountSort();

    // when
    List<Playlist> result =
      findAllByCondition(
        null,
        null,
        null,
        null,
        null,
        "subscriberCount",
        SortDirection.ASCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactly(
        firstPlaylist.getId(),
        secondPlaylist.getId(),
        thirdPlaylist.getId());
  }

  @Test
  @DisplayName("구독자 수를 기준으로 내림차순 조회한다")
  void findAllByCondition_sortBySubscriberCountDescending() {
    // given
    saveSubscriptionsForSubscriberCountSort();

    // when
    List<Playlist> result =
      findAllByCondition(
        null,
        null,
        null,
        null,
        null,
        "subscriberCount",
        SortDirection.DESCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactly(
        thirdPlaylist.getId(),
        secondPlaylist.getId(),
        firstPlaylist.getId());
  }

  @Test
  @DisplayName("구독자 수 오름차순 커서 다음에 위치한 플레이리스트를 조회한다")
  void findAllByCondition_applySubscriberCountCursor() {
    // given
    saveSubscriptionsForSubscriberCountSort();

    // when
    List<Playlist> result =
      findAllByCondition(
        null,
        null,
        null,
        1L,
        secondPlaylist.getId(),
        "subscriberCount",
        SortDirection.ASCENDING);

    // then
    assertThat(result)
      .extracting(Playlist::getId)
      .containsExactly(thirdPlaylist.getId());
  }

  @Test
  @DisplayName("페이지 크기만큼 플레이리스트를 조회한다")
  void findAllByCondition_applyPageSize() {
    // given
    Pageable pageable = PageRequest.of(0, 2);

    // when
    List<Playlist> result =
      playlistRepository.findAllByCondition(
        null,
        null,
        null,
        null,
        null,
        null,
        "updatedAt",
        SortDirection.DESCENDING,
        pageable);

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("조회 조건에 맞는 플레이리스트 개수를 반환한다")
  void countByCondition_returnCount_whenConditionMatches() {
    // when
    long result =
      playlistRepository.countByCondition("추천", owner.getId(), null);

    // then
    assertThat(result).isEqualTo(2L);
  }

  @Test
  @DisplayName("구독자 조건에 맞는 플레이리스트 개수를 반환한다")
  void countByCondition_filterBySubscriberId() {
    // given
    playlistSubscriptionRepository.saveAll(
      List.of(
        new PlaylistSubscription(subscriber, firstPlaylist),
        new PlaylistSubscription(subscriber, thirdPlaylist),
        new PlaylistSubscription(otherSubscriber, secondPlaylist)));

    entityManager.flush();
    entityManager.clear();

    // when
    long result =
      playlistRepository.countByCondition(null, null, subscriber.getId());

    // then
    assertThat(result).isEqualTo(2L);
  }

  @Test
  @DisplayName("조회 조건에 맞는 플레이리스트가 없으면 0을 반환한다")
  void countByCondition_returnZero_whenConditionDoesNotMatch() {
    // when
    long result =
      playlistRepository.countByCondition("존재하지 않는 검색어", null, null);

    // then
    assertThat(result).isZero();
  }

  @Test
  @DisplayName("플레이리스트 ID로 소유자 정보를 포함하여 조회한다")
  void findByIdWithOwner_returnPlaylist_whenExists() {
    // given
    entityManager.clear();

    // when
    Optional<Playlist> result =
      playlistRepository.findByIdWithOwner(firstPlaylist.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(firstPlaylist.getId());
    assertThat(result.get().getOwner().getId()).isEqualTo(owner.getId());

    PersistenceUnitUtil persistenceUnitUtil =
      entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

    assertThat(persistenceUnitUtil.isLoaded(result.get().getOwner())).isTrue();
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트 ID로 조회하면 빈 Optional을 반환한다")
  void findByIdWithOwner_returnEmpty_whenPlaylistDoesNotExist() {
    // when
    Optional<Playlist> result =
      playlistRepository.findByIdWithOwner(UUID.randomUUID());

    // then
    assertThat(result).isEmpty();
  }

  private List<Playlist> findAllByCondition(
    String keywordLike,
    UUID ownerId,
    UUID subscriberId,
    Long subscriberCountCursor,
    UUID idAfter,
    String sortBy,
    SortDirection sortDirection) {

    return playlistRepository.findAllByCondition(
      keywordLike,
      ownerId,
      subscriberId,
      null,
      subscriberCountCursor,
      idAfter,
      sortBy,
      sortDirection,
      DEFAULT_PAGEABLE);
  }

  // 구독자 수 정렬 테스트용 구독 정보 저장
  private void saveSubscriptionsForSubscriberCountSort() {
    playlistSubscriptionRepository.saveAll(
      List.of(
        new PlaylistSubscription(subscriber, secondPlaylist),
        new PlaylistSubscription(subscriber, thirdPlaylist),
        new PlaylistSubscription(otherSubscriber, thirdPlaylist)));

    entityManager.flush();
    entityManager.clear();
  }

  // 테스트용 사용자 생성
  private User createUser(String name, String email) {
    return User.createUser(name, email, "password", null);
  }
}
