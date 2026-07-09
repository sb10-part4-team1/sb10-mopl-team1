package com.sb10.mopl.common.interceptor;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.conversation.entity.ConversationParticipantId;
import com.sb10.mopl.conversation.exception.ConversationErrorCode;
import com.sb10.mopl.conversation.exception.ConversationException;
import com.sb10.mopl.conversation.repository.ConversationParticipantRepository;
import com.sb10.mopl.user.entity.UserRole;
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

  // UUID 형식만 들어올 수 있음
  private static final Pattern DIRECT_MESSAGE_TOPIC_PATTERN =
      Pattern.compile(
          "^/sub/conversations/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/direct-messages$");

  private final JwtProvider jwtProvider;
  private final JwtSessionService jwtSessionService;
  private final ConversationParticipantRepository conversationParticipantRepository;

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

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      authorizeSubscription(accessor);
    }

    if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      log.info("[DM] 세션 종료 감지");
    }

    return message;
  }

  // 대화 참여자가 아닌 사용자가 다른 대화의 DM 토픽을 구독하는 것을 방지
  private void authorizeSubscription(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null) {
      return;
    }

    // UUID 형식인지 검사
    Matcher matcher = DIRECT_MESSAGE_TOPIC_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return;
    }

    UUID conversationId = UUID.fromString(matcher.group(1));
    UUID userId = resolveUserId(accessor.getUser());

    boolean isParticipant =
        conversationParticipantRepository.existsById(
            new ConversationParticipantId(conversationId, userId));
    if (!isParticipant) {
      throw new ConversationException(
          ConversationErrorCode.DIRECT_MESSAGE_TOPIC_ACCESS_DENIED,
          Map.of("conversationId", conversationId, "userId", userId));
    }
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
      AuthenticatedUser authenticatedUser = toAuthenticatedUser(claims);

      UUID sessionId = UUID.fromString(requiredClaim(claims, JwtProvider.SESSION_ID_CLAIM));
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

  // JWT claim들을 검증하면서 AuthenticatedUser로 변환한다
  private AuthenticatedUser toAuthenticatedUser(Claims claims) {
    String subject = claims.getSubject();
    String id = requiredClaim(claims, "id");
    String email = requiredClaim(claims, "email");
    String role = requiredClaim(claims, "role");
    String tokenType = requiredClaim(claims, JwtProvider.TOKEN_TYPE_CLAIM);

    if (subject == null || subject.isBlank() || !subject.equals(id)) {
      log.warn("[DM] JWT subject가 id claim과 일치하지 않습니다. subject={}, id={}", subject, id);
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
    }
    if (!JwtProvider.ACCESS_TOKEN_TYPE.equals(tokenType)) {
      log.warn("[DM] ACCESS 토큰이 아닙니다. tokenType={}", tokenType);
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
    }

    return new AuthenticatedUser(UUID.fromString(id), email, UserRole.valueOf(role));
  }

  // jwt의 필수 claim을 조회하고, 없다면 예외를 던진다.
  private String requiredClaim(Claims claims, String name) {
    Object value = claims.get(name);

    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      log.warn("[DM] JWT claim이 누락되었습니다: {}", name);
      throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
    }

    return stringValue;
  }
}
