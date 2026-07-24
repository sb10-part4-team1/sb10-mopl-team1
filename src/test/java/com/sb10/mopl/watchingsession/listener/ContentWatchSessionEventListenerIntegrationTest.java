package com.sb10.mopl.watchingsession.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.principal.MoplUserDetails;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import com.sb10.mopl.watchingsession.repository.WatchingSessionRepository;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
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

    assertThat(watchingSessionRepository.findByWatcherId(user.getId()))
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
    assertThat(watchingSessionRepository.findByWatcherId(user.getId()))
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

    assertThat(watchingSessionRepository.findByWatcherId(user.getId())).isPresent();

    // 새로고침 등으로 탭 하나(기존 연결)만 끊긴 상황
    tab1.disconnect();
    Thread.sleep(300);

    assertThat(watchingSessionRepository.findByWatcherId(user.getId()))
        .as("다른 탭(tab2)이 같은 콘텐츠를 계속 보고 있으므로 시청 세션이 유지되어야 한다")
        .isPresent();
    assertThat(contentRepository.findById(content.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(1);

    // 마지막 탭도 끊기면 그때 비로소 시청 세션이 종료된다
    tab2.disconnect();
    Thread.sleep(300);

    assertThat(watchingSessionRepository.findByWatcherId(user.getId())).isEmpty();
    assertThat(contentRepository.findById(content.getId()).orElseThrow().getWatcherCount())
        .isEqualTo(0);
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
