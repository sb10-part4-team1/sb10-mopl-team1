package com.sb10.mopl.common.interceptor;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.jwt.AuthenticatedUserFactory;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.conversation.exception.ConversationErrorCode;
import com.sb10.mopl.conversation.exception.ConversationException;
import com.sb10.mopl.conversation.repository.ConversationParticipantRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/** connect 프레임의 jwt 검증해 세션에 인증 정보를 부여하고 sub 프레임에서는 dm 토픽 구독자가 실제 대화 참여자인지 검사합니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String PUBLISH_PREFIX = "/pub";

  // Spring 기본 사용자 목적지 프리픽스
  private static final String USER_DESTINATION_PREFIX = "/user/";

  private static final String UUID_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

  // UUID 형식만 들어올 수 있음
  private static final Pattern DIRECT_MESSAGE_TOPIC_PATTERN =
      Pattern.compile("^/sub/conversations/(" + UUID_PATTERN + ")/direct-messages$");

  // UUID 형식만 들어올 수 있음. 시청 세션(watch)과 실시간 채팅(chat)은 둘 다 콘텐츠 존재 여부만 검증하면 됩니다.
  private static final Pattern CONTENT_TOPIC_PATTERN =
      Pattern.compile("^/sub/contents/(" + UUID_PATTERN + ")/(?:watch|chat)$");

  private final JwtProvider jwtProvider;
  private final JwtSessionService jwtSessionService;
  private final AuthenticatedUserFactory authenticatedUserFactory;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final ContentRepository contentRepository;

  // stomp 프레임 종류에 따라 connect 인증, sub는 구독 권한을 검사
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String sessionId = accessor.getSessionId();
      accessor.setUser(authenticate(accessor));
      log.info("[DM] 새로운 STOMP 연결: {}", sessionId);
    }

    // send 프레임 검증
    if (StompCommand.SEND.equals(accessor.getCommand())) {
      authorizeSend(accessor);
    }

    // subscribe 프레임 검증 및 거부된 구독만 중단
    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && !authorizeSubscription(accessor)) {
      return null;
    }

    if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      log.info("[DM] 세션 종료 감지");
    }

    return message;
  }

  // 클라이언트가 브로커 prefix(/sub)로 직접 SEND해 @MessageMapping(DB 저장/검증/참여자 확인)을 우회하고
  // 다른 구독자에게 임의의 메시지를 주입하는 것을 방지합니다. SEND는 반드시 /pub로만 들어와야 합니다.
  private void authorizeSend(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith(PUBLISH_PREFIX)) {
      log.warn("[DM] 허용되지 않은 SEND 엔드포인트입니다. destination={}", destination);
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
    }
  }

  // destination 패턴에 맞는 토픽별 구독 권한 검사로 위임합니다.
  // false를 반환하면 해당 SUBSCRIBE 메시지만 무시합니다.
  private boolean authorizeSubscription(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();

    if (destination == null) {
      rejectSubscription(null);
      return false;
    }

    // 사용자 전용 목적지는 기존과 동일하게 허용
    if (destination.startsWith(USER_DESTINATION_PREFIX)) {
      return true;
    }

    // 대화방 구독은 기존과 동일하게 참여 여부를 검증
    Matcher directMessageMatcher = DIRECT_MESSAGE_TOPIC_PATTERN.matcher(destination);
    if (directMessageMatcher.matches()) {
      authorizeDirectMessageSubscription(directMessageMatcher, accessor);
      return true;
    }

    // UUID 형식이 올바른 콘텐츠 구독
    Matcher contentTopicMatcher = CONTENT_TOPIC_PATTERN.matcher(destination);
    if (contentTopicMatcher.matches()) {
      return authorizeContentTopicSubscription(contentTopicMatcher);
    }

    // /sub/contents/로 시작하지만 UUID 형식 또는 목적지 형식이 잘못된 경우
    // 연결을 끊지 않고 해당 SUBSCRIBE만 거부
    if (destination.startsWith("/sub/contents/")) {
      log.warn("[STOMP] 잘못된 콘텐츠 구독을 무시합니다. destination={}", destination);
      return false;
    }

    // 콘텐츠 이외의 알 수 없는 목적지는 기존 정책대로 예외 처리
    rejectSubscription(destination);
    return false;
  }

  // 대화 참여자가 아닌 사용자가 다른 대화의 DM 토픽을 구독하는 것을 방지
  private void authorizeDirectMessageSubscription(Matcher matcher, StompHeaderAccessor accessor) {
    UUID conversationId = UUID.fromString(matcher.group(1));
    UUID userId = resolveUserId(accessor.getUser());

    boolean isParticipant =
        conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, userId);
    if (!isParticipant) {
      throw new ConversationException(
          ConversationErrorCode.DIRECT_MESSAGE_TOPIC_ACCESS_DENIED,
          Map.of("conversationId", conversationId, "userId", userId));
    }
  }

  // 존재하는 콘텐츠의 시청 세션/채팅 토픽만 구독하도록 허용합니다.
  // 존재하지 않으면 예외를 던지지 않고 해당 SUBSCRIBE만 거부합니다.
  private boolean authorizeContentTopicSubscription(Matcher matcher) {
    UUID contentId = UUID.fromString(matcher.group(1));

    if (!contentRepository.existsById(contentId)) {
      log.warn("[STOMP] 존재하지 않는 콘텐츠 구독을 무시합니다. contentId={}", contentId);
      return false;
    }

    return true;
  }

  private void rejectSubscription(String destination) {
    log.warn("[STOMP] 허용되지 않은 SUBSCRIBE 목적지입니다. destination={}", destination);
    throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
  }

  // sub 프레임에 저장된 principal에서 인증된 사용자의 id를 구한다
  private UUID resolveUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser.id();
    }

    log.warn("[DM] 인증 정보가 없는 구독 요청입니다.");

    throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
  }

  // Authorization 헤더를 검증해 stomp 세션에 인증 정보를 부여한다.
  private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
    String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      log.warn("[DM] Bearer 액세스 토큰이 필요합니다.");
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();

    if (token.isBlank()) {
      log.warn("[DM] 액세스 토큰이 비어 있습니다.");
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
    }

    try {
      Claims claims = jwtProvider.parseClaims(token);
      AuthenticatedUser authenticatedUser = authenticatedUserFactory.from(claims);

      UUID sessionId =
          UUID.fromString(
              authenticatedUserFactory.requiredClaim(claims, JwtProvider.SESSION_ID_CLAIM));
      if (!jwtSessionService.isActive(authenticatedUser.id(), sessionId)) {
        log.warn(
            "[DM] JWT 세션이 유효하지 않습니다. userId={}, sessionId={}", authenticatedUser.id(), sessionId);
        throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
      }

      return UsernamePasswordAuthenticationToken.authenticated(
          authenticatedUser,
          null,
          List.of(new SimpleGrantedAuthority(authenticatedUser.authorityName())));
    } catch (JwtException | IllegalArgumentException e) {

      log.warn("[DM] 유효하지 않은 WebSocket 인증 토큰입니다.", e);
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of(), e);
    }
  }
}
