package com.sb10.mopl.watchingsession.listener;

import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.watchingsession.dto.ChangeType;
import com.sb10.mopl.watchingsession.dto.WatchingSessionChange;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * 콘텐츠 시청 세션(/sub/contents/{contentId}/watch)의 구독 생명주기를 감지해 시청 참여(JOIN)/이탈(LEAVE)을 기록하고 같은 토픽 구독자에게
 * 브로드캐스트합니다.
 *
 * <p>구독 해제(UNSUBSCRIBE)와 연결 종료(DISCONNECT)에는 destination 정보가 없으므로, 구독 시점에 subscriptionId를 기준으로 어떤
 * 콘텐츠를 보고 있었는지 메모리에 추적해 둡니다.
 *
 * <p>단일 인스턴스 기준 구현이며, 다중 인스턴스로 확장 시 이 추적 상태는 Redis 등 공유 저장소로 옮겨야 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentWatchSessionEventListener {

  private static final Pattern CONTENT_WATCH_TOPIC_PATTERN =
      Pattern.compile(
          "^/sub/contents"
              + "/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"
              + "/watch$");

  private final WatchingSessionService watchingSessionService;
  private final SimpMessagingTemplate messagingTemplate;

  private final Map<String, WatchSubscription> subscriptionsBySubscriptionId =
      new ConcurrentHashMap<>();

  // handleDisconnect에서 sessionId로 구독을 O(1)에 찾기 위한 보조 인덱스
  private final Map<String, Set<String>> subscriptionIdsBySessionId = new ConcurrentHashMap<>();

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();
    String subscriptionId = accessor.getSubscriptionId();
    String sessionId = accessor.getSessionId();

    if (destination == null || subscriptionId == null || sessionId == null) {
      return;
    }

    Matcher matcher = CONTENT_WATCH_TOPIC_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return;
    }

    UUID contentId = UUID.fromString(matcher.group(1));
    UUID watcherId = resolveUserId(accessor.getUser());
    if (watcherId == null) {
      return;
    }

    WatchingSessionDto dto = watchingSessionService.join(watcherId, contentId);
    track(subscriptionId, new WatchSubscription(sessionId, contentId, watcherId));

    log.info("[WATCH] 시청 참여: contentId={}, watcherId={}", contentId, watcherId);
    broadcast(contentId, ChangeType.JOIN, dto);
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String subscriptionId = accessor.getSubscriptionId();
    if (subscriptionId == null) {
      return;
    }

    WatchSubscription subscription = untrack(subscriptionId);
    if (subscription != null) {
      leave(subscription);
    }
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();

    Set<String> subscriptionIds = subscriptionIdsBySessionId.remove(sessionId);
    if (subscriptionIds == null) {
      return;
    }

    for (String subscriptionId : subscriptionIds) {
      WatchSubscription subscription = subscriptionsBySubscriptionId.remove(subscriptionId);
      if (subscription != null) {
        leave(subscription);
      }
    }
  }

  // subscriptionsBySubscriptionId와 subscriptionIdsBySessionId(보조 인덱스)를 함께 갱신합니다.
  private void track(String subscriptionId, WatchSubscription subscription) {
    subscriptionsBySubscriptionId.put(subscriptionId, subscription);
    subscriptionIdsBySessionId
        .computeIfAbsent(subscription.sessionId(), key -> ConcurrentHashMap.newKeySet())
        .add(subscriptionId);
  }

  // 보조 인덱스에서도 함께 제거하며, 세션의 마지막 구독이었다면 세션 항목 자체도 정리합니다.
  private WatchSubscription untrack(String subscriptionId) {
    WatchSubscription subscription = subscriptionsBySubscriptionId.remove(subscriptionId);
    if (subscription != null) {
      subscriptionIdsBySessionId.computeIfPresent(
          subscription.sessionId(),
          (sessionId, subscriptionIds) -> {
            subscriptionIds.remove(subscriptionId);
            return subscriptionIds.isEmpty() ? null : subscriptionIds;
          });
    }
    return subscription;
  }

  private void leave(WatchSubscription subscription) {
    Optional<WatchingSessionDto> dto =
        watchingSessionService.leave(subscription.watcherId(), subscription.contentId());

    dto.ifPresent(
        watchingSessionDto -> {
          log.info(
              "[WATCH] 시청 이탈: contentId={}, watcherId={}",
              subscription.contentId(),
              subscription.watcherId());
          broadcast(subscription.contentId(), ChangeType.LEAVE, watchingSessionDto);
        });
  }

  private void broadcast(UUID contentId, ChangeType type, WatchingSessionDto dto) {
    long watcherCount = watchingSessionService.countWatchers(contentId);
    messagingTemplate.convertAndSend(
        "/sub/contents/" + contentId + "/watch",
        new WatchingSessionChange(type, dto, watcherCount));
  }

  // CONNECT 시점에 StompChannelInterceptor가 세션에 부여한 Principal에서 시청자를 꺼낸다.
  // 이 시점의 구독은 이미 StompChannelInterceptor의 인증 검사를 통과했으므로 정상적으로는 항상 존재한다.
  private UUID resolveUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser.id();
    }

    log.warn("[WATCH] 인증 정보가 없는 구독 요청입니다.");
    return null;
  }

  private record WatchSubscription(String sessionId, UUID contentId, UUID watcherId) {}
}
