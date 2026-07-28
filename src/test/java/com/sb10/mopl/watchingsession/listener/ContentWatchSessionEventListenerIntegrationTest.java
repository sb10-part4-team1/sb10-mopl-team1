package com.sb10.mopl.watchingsession.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.principal.MoplUserDetails;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import com.sb10.mopl.watchingsession.dto.ChangeType;
import com.sb10.mopl.watchingsession.dto.WatchingSessionChange;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import com.sb10.mopl.watchingsession.repository.WatchingSessionRepository;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * 유저당 시청 세션이 하나만 유지되는지(콘텐츠 이동), 그리고 같은 콘텐츠를 여러 탭에서 보고 있을 때 그중 하나만 끊겨도 시청 세션이 유지되는지(새로고침 등으로 인한 연결
 * 겹침 대응)를 검증하는 통합 테스트입니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ContentWatchSessionEventListenerIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private UserRepository userRepository;
  @Autowired private JwtSessionRepository jwtSessionRepository;
  @Autowired private JwtSessionService jwtSessionService;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ContentRepository contentRepository;
  @Autowired private WatchingSessionRepository watchingSessionRepository;

  private WebSocketStompClient stompClient;

  @BeforeEach
  void setUp() {
    watchingSessionRepository.deleteAll();
    contentRepository.deleteAll();
    jwtSessionRepository.deleteAll();
    userRepository.deleteAll();

    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
    messageConverter.getObjectMapper().registerModule(new JavaTimeModule());
    stompClient.setMessageConverter(messageConverter);
  }

  @AfterEach
  void tearDown() {
    watchingSessionRepository.deleteAll();
    contentRepository.deleteAll();
    jwtSessionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("다른 콘텐츠를 구독하면 기존 시청 세션이 이동하고, 이전 콘텐츠의 시청자 수는 줄어든다")
  void subscribe_moveSession_whenWatchingDifferentContent() throws Exception {
    User user = createUser("mover@example.com");
    Content contentA = createContent("스파이더맨");
    String token = issueAccessToken(user);

    StompSession session = connect(token);

    session.subscribe(
        "/sub/contents/" + contentA.getId() + "/watch", new StompSessionHandlerAdapter() {});
    Thread.sleep(300);

    assertThat(findSessionByWatcherId(user.getId()))
        .map(WatchingSession::getContent)
        .map(Content::getId)
        .contains(contentA.getId());
    assertThat(contentRepository.findById(contentA.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(1);

    Content contentB = createContent("앤트맨");
    session.subscribe(
        "/sub/contents/" + contentB.getId() + "/watch", new StompSessionHandlerAdapter() {});
    Thread.sleep(300);

    // 유저당 시청 세션은 항상 1개: 이전 콘텐츠(A)에 남아있지 않고 새 콘텐츠(B)로 이동했다.
    assertThat(findSessionByWatcherId(user.getId()))
        .map(WatchingSession::getContent)
        .map(Content::getId)
        .contains(contentB.getId());
    assertThat(watchingSessionRepository.count()).isEqualTo(1);
    assertThat(contentRepository.findById(contentA.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(0);
    assertThat(contentRepository.findById(contentB.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(1);

    session.disconnect();
  }

  @Test
  @DisplayName("같은 콘텐츠를 여러 탭에서 보고 있으면, 그중 하나가 끊겨도 시청 세션이 유지된다")
  void unsubscribe_keepSession_whenAnotherTabStillWatchingSameContent() throws Exception {
    User user = createUser("multi-tab@example.com");
    Content content = createContent("아이언맨");
    String token = issueAccessToken(user);

    StompSession tab1 = connect(token);
    tab1.subscribe(
        "/sub/contents/" + content.getId() + "/watch", new StompSessionHandlerAdapter() {});
    Thread.sleep(300);

    StompSession tab2 = connect(token);
    tab2.subscribe(
        "/sub/contents/" + content.getId() + "/watch", new StompSessionHandlerAdapter() {});
    Thread.sleep(300);

    assertThat(findSessionByWatcherId(user.getId())).isPresent();

    // 새로고침 등으로 탭 하나(기존 연결)만 끊긴 상황
    tab1.disconnect();
    Thread.sleep(300);

    assertThat(findSessionByWatcherId(user.getId()))
        .as("다른 탭(tab2)이 같은 콘텐츠를 계속 보고 있으므로 시청 세션이 유지되어야 한다")
        .isPresent();
    assertThat(contentRepository.findById(content.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(1);

    // 마지막 탭도 끊기면 그때 비로소 시청 세션이 종료된다
    tab2.disconnect();
    Thread.sleep(300);

    assertThat(findSessionByWatcherId(user.getId())).isEmpty();
    assertThat(contentRepository.findById(content.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(0);
  }

  @Test
  @DisplayName("인증되지 않은 연결은 콘텐츠 구독 자체가 거부되어 시청 세션이 생성되지 않는다")
  void connect_neverCreateSession_whenNotAuthenticated() {
    Content content = createContent("배트맨");

    StompHeaders connectHeaders = new StompHeaders(); // Authorization 헤더 없음

    assertThatThrownBy(
            () ->
                stompClient
                    .connectAsync(
                        "ws://localhost:{port}/ws/websocket",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {},
                        port)
                    .get(5, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class);

    assertThat(watchingSessionRepository.findAll()).isEmpty();
    assertThat(contentRepository.findById(content.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(0);
  }

  @Test
  @DisplayName("구독 시점에 본인의 JOIN 결과를 구독 응답으로 직접 받는다")
  void subscribe_receivesOwnJoinResultAsDirectReply() throws Exception {
    User user = createUser("direct-reply@example.com");
    Content content = createContent("직접응답테스트");
    String token = issueAccessToken(user);

    StompSession session = connect(token);
    CompletableFuture<WatchingSessionChange> received = new CompletableFuture<>();

    session.subscribe(
        "/sub/contents/" + content.getId() + "/watch",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return WatchingSessionChange.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            received.complete((WatchingSessionChange) payload);
          }
        });

    WatchingSessionChange change = received.get(5, TimeUnit.SECONDS);

    // REST 목록 조회 등 다른 요청과 경합할 필요 없이, 구독 자체의 응답만으로 본인의 참여 사실을 확정적으로 받는다.
    assertThat(change.type()).isEqualTo(ChangeType.JOIN);
    assertThat(change.watchingSession().watcher().userId()).isEqualTo(user.getId());
    assertThat(change.watchingSession().content().id()).isEqualTo(content.getId());
    assertThat(change.watcherCount()).isEqualTo(1);

    session.disconnect();
    Thread.sleep(300);
  }

  @Test
  @DisplayName("같은 유저가 두 연결에서 동시에 처음 구독해도 시청 세션은 정확히 하나만 생성된다")
  void subscribe_createExactlyOneSession_whenFirstSubscribingConcurrentlyFromTwoConnections()
      throws Exception {
    User user = createUser("concurrent@example.com");
    Content content = createContent("어벤져스");
    String token = issueAccessToken(user);

    StompSession tab1 = connect(token);
    StompSession tab2 = connect(token);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<?>> futures =
          List.of(
              executor.submit(subscribeTask(tab1, content.getId(), ready, start)),
              executor.submit(subscribeTask(tab2, content.getId(), ready, start)));

      // 두 요청이 최대한 같은 시점에 SUBSCRIBE를 보내도록 동기화한다.
      ready.await(5, TimeUnit.SECONDS);
      start.countDown();

      for (Future<?> future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdown();
    }
    Thread.sleep(300);

    assertThat(
            watchingSessionRepository.findAll().stream()
                .filter(session -> session.getWatcher().getId().equals(user.getId()))
                .count())
        .isEqualTo(1);
    assertThat(contentRepository.findById(content.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(1);

    tab1.disconnect();
    tab2.disconnect();
  }

  private Callable<Void> subscribeTask(
      StompSession session, UUID contentId, CountDownLatch ready, CountDownLatch start) {
    return () -> {
      ready.countDown();
      start.await();
      session.subscribe(
          "/sub/contents/" + contentId + "/watch", new StompSessionHandlerAdapter() {});
      return null;
    };
  }

  // watchingSessionRepository.findByWatcherId()는 PESSIMISTIC_WRITE 락을 걸어 쓰기 트랜잭션이
  // 필요하므로, 트랜잭션 없이 상태만 확인하는 테스트 어서션에서는 findAll() 기반으로 조회한다.
  private Optional<WatchingSession> findSessionByWatcherId(UUID watcherId) {
    return watchingSessionRepository.findAll().stream()
        .filter(session -> session.getWatcher().getId().equals(watcherId))
        .findFirst();
  }

  private User createUser(String email) {
    return userRepository.saveAndFlush(
        User.createUser("watch-test-user", email, passwordEncoder.encode("password123"), null));
  }

  private Content createContent(String title) {
    return contentRepository.saveAndFlush(
        Content.create(title, ContentType.MOVIE, "설명", "http://example.com/thumb.png"));
  }

  private String issueAccessToken(User user) {
    JwtSessionService.IssuedJwtSession session = jwtSessionService.issue(user.getId());
    return jwtProvider.createAccessToken(new MoplUserDetails(user), session.sessionId());
  }

  private StompSession connect(String token) throws Exception {
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);

    return stompClient
        .connectAsync(
            "ws://localhost:{port}/ws/websocket",
            new WebSocketHttpHeaders(),
            connectHeaders,
            new StompSessionHandlerAdapter() {},
            port)
        .get(5, TimeUnit.SECONDS);
  }
}
