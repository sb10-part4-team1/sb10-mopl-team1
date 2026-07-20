package com.sb10.mopl.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.common.exception.ErrorResponse;
import com.sb10.mopl.content.dto.ContentChatSendRequest;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * MoplException으로 잡히지 않는 STOMP 예외 경로(@Valid 검증 실패, 채널 인터셉터 단계 예외)가 실제 서버에서 의도한 대로 처리되는지 확인하는 통합
 * 테스트입니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StompExceptionHandlingIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private UserRepository userRepository;
  @Autowired private JwtSessionRepository jwtSessionRepository;
  @Autowired private JwtSessionService jwtSessionService;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private StompSession stompSession;

  @BeforeEach
  void setUp() {
    jwtSessionRepository.deleteAll();
    userRepository.deleteAll();

    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
  }

  @AfterEach
  void tearDown() {
    if (stompSession != null && stompSession.isConnected()) {
      stompSession.disconnect();
    }
  }

  @Test
  @DisplayName("@Valid 검증 실패를 /user/sub/queue/errors로 응답한다")
  void send_respondWithValidationError_whenPayloadIsBlank() throws Exception {
    String token = issueAccessToken("chat-user@example.com");
    stompSession = connect(token, new StompSessionHandlerAdapter() {});

    BlockingQueue<ErrorResponse> errors = new LinkedBlockingQueue<>();
    stompSession.subscribe(
        "/user/sub/queue/errors",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return ErrorResponse.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            errors.add((ErrorResponse) payload);
          }
        });

    // 구독이 브로커에 등록될 시간을 확보
    Thread.sleep(300);

    stompSession.send(
        "/pub/contents/" + UUID.randomUUID() + "/chat", new ContentChatSendRequest(" "));

    ErrorResponse errorResponse = errors.poll(5, TimeUnit.SECONDS);

    assertThat(errorResponse).isNotNull();
    assertThat(errorResponse.code()).isEqualTo("SYS01");
    assertThat(errorResponse.details()).containsKey("content");
  }

  @Test
  @DisplayName("채널 인터셉터 단계의 예외를 STOMP ERROR 프레임으로 응답한다")
  void subscribe_respondWithErrorFrame_whenContentDoesNotExist() throws Exception {
    String token = issueAccessToken("subscribe-user@example.com");

    BlockingQueue<byte[]> errorFrames = new LinkedBlockingQueue<>();
    StompSessionHandler handler =
        new StompSessionHandlerAdapter() {
          @Override
          public void handleException(
              StompSession session,
              StompCommand command,
              StompHeaders headers,
              byte[] payload,
              Throwable exception) {
            errorFrames.add(payload);
          }
        };
    stompSession = connect(token, handler);

    // 존재하지 않는 콘텐츠 구독 -> StompChannelInterceptor.preSend에서 ContentException 발생
    stompSession.subscribe(
        "/sub/contents/" + UUID.randomUUID() + "/chat", new StompSessionHandlerAdapter() {});

    byte[] payload = errorFrames.poll(5, TimeUnit.SECONDS);
    assertThat(payload).isNotNull();

    ErrorResponse errorResponse = objectMapper.readValue(payload, ErrorResponse.class);
    assertThat(errorResponse.code()).isEqualTo("CT01");
  }

  @Test
  @DisplayName("CONNECT에 Bearer 토큰이 없으면 AUTH01 ERROR 프레임으로 응답한다")
  void connect_respondWithErrorFrame_whenBearerTokenIsMissing() throws Exception {
    StompHeaders connectHeaders = new StompHeaders(); // Authorization 헤더 없음

    BlockingQueue<byte[]> errorFrames = new LinkedBlockingQueue<>();
    StompSessionHandler handler =
        new StompSessionHandlerAdapter() {
          @Override
          public void handleException(
              StompSession session,
              StompCommand command,
              StompHeaders headers,
              byte[] payload,
              Throwable exception) {
            errorFrames.add(payload);
          }
        };

    // CONNECTED 프레임을 받지 못하므로 connectAsync().get()은 예외로 완료된다.
    assertThatThrownBy(
            () ->
                stompClient
                    .connectAsync(
                        "ws://localhost:{port}/ws/websocket",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        handler,
                        port)
                    .get(5, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class);

    byte[] payload = errorFrames.poll(5, TimeUnit.SECONDS);
    assertThat(payload).isNotNull();

    ErrorResponse errorResponse = objectMapper.readValue(payload, ErrorResponse.class);
    assertThat(errorResponse.code()).isEqualTo("AUTH01");
  }

  private String issueAccessToken(String email) {
    User user =
        userRepository.saveAndFlush(
            User.createUser("stomp-test-user", email, passwordEncoder.encode("password123"), null));
    JwtSessionService.IssuedJwtSession session = jwtSessionService.issue(user.getId());
    return jwtProvider.createAccessToken(new MoplUserDetails(user), session.sessionId());
  }

  private StompSession connect(String token, StompSessionHandler handler) throws Exception {
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);

    return stompClient
        .connectAsync(
            "ws://localhost:{port}/ws/websocket",
            new WebSocketHttpHeaders(),
            connectHeaders,
            handler,
            port)
        .get(5, TimeUnit.SECONDS);
  }
}
