package com.sb10.mopl.common.interceptor;

import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.conversation.entity.ConversationParticipantId;
import com.sb10.mopl.conversation.repository.ConversationParticipantRepository;
import com.sb10.mopl.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.security.Principal;
import java.util.List;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private static final Pattern DIRECT_MESSAGE_TOPIC_PATTERN =
      Pattern.compile("^/sub/conversations/([0-9a-fA-F-]{36})/direct-messages$");

  private final JwtProvider jwtProvider;
  private final JwtSessionService jwtSessionService;
  private final ConversationParticipantRepository conversationParticipantRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String sessionId = accessor.getSessionId();
      accessor.setUser(authenticate(accessor));
      log.info("새로운 STOMP 연결: {}", sessionId);
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      authorizeSubscription(accessor);
    }

    if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      log.info("세션 종료 감지");
    }

    return message;
  }

  // 대화 참여자가 아닌 사용자가 다른 대화의 DM 토픽을 구독해 메시지를 가로채는 것을 방지합니다.
  private void authorizeSubscription(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null) {
      return;
    }

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
      throw new IllegalArgumentException("해당 대화의 참여자만 구독할 수 있습니다.");
    }
  }

  private UUID resolveUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser.id();
    }
    throw new IllegalArgumentException("인증 정보가 없는 구독 요청입니다.");
  }

  // CONNECT 프레임의 Authorization 헤더를 검증해 STOMP 세션에 인증 정보를 부여합니다.
  // 이후 같은 세션의 SEND/SUBSCRIBE 프레임에서 Principal로 재사용됩니다.
  private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
    String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw new IllegalArgumentException("WebSocket 연결에는 Bearer 액세스 토큰이 필요합니다.");
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (token.isBlank()) {
      throw new IllegalArgumentException("액세스 토큰이 비어 있습니다.");
    }

    try {
      Claims claims = jwtProvider.parseClaims(token);
      AuthenticatedUser authenticatedUser = toAuthenticatedUser(claims);

      UUID sessionId = UUID.fromString(requiredClaim(claims, JwtProvider.SESSION_ID_CLAIM));
      if (!jwtSessionService.isActive(authenticatedUser.id(), sessionId)) {
        throw new IllegalArgumentException("JWT 세션이 유효하지 않습니다.");
      }

      return UsernamePasswordAuthenticationToken.authenticated(
          authenticatedUser,
          null,
          List.of(new SimpleGrantedAuthority(authenticatedUser.authorityName())));
    } catch (JwtException | IllegalArgumentException e) {
      throw new IllegalArgumentException("유효하지 않은 WebSocket 인증 토큰입니다.", e);
    }
  }

  private AuthenticatedUser toAuthenticatedUser(Claims claims) {
    String subject = claims.getSubject();
    String id = requiredClaim(claims, "id");
    String email = requiredClaim(claims, "email");
    String role = requiredClaim(claims, "role");
    String tokenType = requiredClaim(claims, JwtProvider.TOKEN_TYPE_CLAIM);

    if (subject == null || subject.isBlank() || !subject.equals(id)) {
      throw new IllegalArgumentException("JWT subject가 id claim과 일치하지 않습니다.");
    }
    if (!JwtProvider.ACCESS_TOKEN_TYPE.equals(tokenType)) {
      throw new IllegalArgumentException("ACCESS 토큰이 아닙니다.");
    }

    return new AuthenticatedUser(UUID.fromString(id), email, UserRole.valueOf(role));
  }

  private String requiredClaim(Claims claims, String name) {
    Object value = claims.get(name);
    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      throw new IllegalArgumentException("JWT claim이 누락되었습니다: " + name);
    }
    return stringValue;
  }
}
