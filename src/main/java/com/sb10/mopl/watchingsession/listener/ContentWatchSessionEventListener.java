package com.sb10.mopl.watchingsession.listener;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.watchingsession.dto.ChangeType;
import com.sb10.mopl.watchingsession.dto.WatchingSessionChange;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.dto.WatchingSessionJoinResult;
import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
 * <p>구독 해제(UNSUBSCRIBE)와 연결 종료(DISCONNECT)에는 destination 정보가 없으므로, 구독 시점에 (sessionId,
 * subscriptionId)를 기준으로 어떤 콘텐츠를 보고 있었는지 메모리에 추적해 둡니다. subscriptionId는 하나의 STOMP 연결 내에서만
 * 유일하므로(예: 서로 다른 탭이 각각 "sub-0"을 부여할 수 있음) sessionId 없이는 다른 연결의 구독과 충돌할 수 있습니다.
 *
 * <p>유저는 동시에 하나의 콘텐츠만 시청할 수 있어(watching_session.watcher_id 유니크), 다른 콘텐츠를 구독하면 세션이
 * 이동하며 이전 콘텐츠 구독자에게도 LEAVE가 브로드캐스트됩니다. 같은 (watcherId, contentId)에 대해 남아있는 활성 구독 수를 세어,
 * 새로고침 등으로 옛 연결과 새 연결이 겹치는 구간에도 마지막 구독이 끊길 때만 실제로 시청 세션을 종료합니다.
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

  // subscriptionId는 하나의 STOMP 세션(연결) 안에서만 유일하므로, 서로 다른 연결(탭)의 구독을 구분하기 위해
  // sessionId까지 포함한 복합 키로 관리한다.
  private final Map<SubscriptionKey, WatchSubscription> subscriptionsByKey = new ConcurrentHashMap<>();

  // handleDisconnect에서 sessionId로 구독을 O(1)에 찾기 위한 보조 인덱스
  private final Map<String, Set<String>> subscriptionIdsBySessionId = new ConcurrentHashMap<>();

  // 같은 (watcherId, contentId)를 보고 있는 활성 구독(탭/연결) 수. 새로고침 등으로 발생하는 겹침 구간에서
  // 마지막 구독이 끊길 때만 실제로 시청 세션을 종료하기 위해 둔다.
  private final Map<WatcherContentKey, AtomicInteger> subscriberCounts = new ConcurrentHashMap<>();

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

    WatchingSessionJoinResult result = watchingSessionService.join(watcherId, contentId);
    track(sessionId, subscriptionId, new WatchSubscription(sessionId, contentId, watcherId));
    incrementSubscriberCount(watcherId, contentId);

    if (result.previousSession() != null) {
      UUID previousContentId = result.previousSession().content().id();
      log.info(
          "[WATCH] 시청 이동: {} -> {}, watcherId={}", previousContentId, contentId, watcherId);
      broadcast(previousContentId, ChangeType.LEAVE, result.previousSession());
    }

    log.info("[WATCH] 시청 참여: contentId={}, watcherId={}", contentId, watcherId);
    broadcast(contentId, ChangeType.JOIN, result.session());
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String subscriptionId = accessor.getSubscriptionId();
    String sessionId = accessor.getSessionId();
    if (subscriptionId == null || sessionId == null) {
      return;
    }

    WatchSubscription subscription = untrack(sessionId, subscriptionId);
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
      WatchSubscription subscription =
          subscriptionsByKey.remove(new SubscriptionKey(sessionId, subscriptionId));
      if (subscription != null) {
        leave(subscription);
      }
    }
  }

  // subscriptionsByKey와 subscriptionIdsBySessionId(보조 인덱스)를 함께 갱신합니다.
  private void track(String sessionId, String subscriptionId, WatchSubscription subscription) {
    subscriptionsByKey.put(new SubscriptionKey(sessionId, subscriptionId), subscription);
    subscriptionIdsBySessionId
        .computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
        .add(subscriptionId);
  }

  // 보조 인덱스에서도 함께 제거하며, 세션의 마지막 구독이었다면 세션 항목 자체도 정리합니다.
  private WatchSubscription untrack(String sessionId, String subscriptionId) {
    WatchSubscription subscription =
        subscriptionsByKey.remove(new SubscriptionKey(sessionId, subscriptionId));
    if (subscription != null) {
      subscriptionIdsBySessionId.computeIfPresent(
          sessionId,
          (key, subscriptionIds) -> {
            subscriptionIds.remove(subscriptionId);
            return subscriptionIds.isEmpty() ? null : subscriptionIds;
          });
    }
    return subscription;
  }

  private void leave(WatchSubscription subscription) {
    int remaining = decrementSubscriberCount(subscription.watcherId(), subscription.contentId());
    if (remaining > 0) {
      // 같은 콘텐츠를 보고 있는 다른 탭/연결이 남아있으므로(새로고침 등으로 인한 일시적 겹침 포함)
      // 시청 세션을 유지한다.
      return;
    }

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

  // 구독 시작 시 (watcherId, contentId) 기준 활성 구독 수를 1 증가시킨다.
  private void incrementSubscriberCount(UUID watcherId, UUID contentId) {
    subscriberCounts
        .computeIfAbsent(new WatcherContentKey(watcherId, contentId), key -> new AtomicInteger())
        .incrementAndGet();
  }

  // 구독 종료 시 활성 구독 수를 1 감소시키고 남은 수를 반환한다. 0 이하가 되면 맵에서 제거한다.
  private int decrementSubscriberCount(UUID watcherId, UUID contentId) {
    WatcherContentKey key = new WatcherContentKey(watcherId, contentId);
    AtomicInteger count = subscriberCounts.get(key);
    if (count == null) {
      return 0;
    }

    int remaining = count.decrementAndGet();
    if (remaining <= 0) {
      subscriberCounts.remove(key, count);
    }
    return remaining;
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

  private record WatcherContentKey(UUID watcherId, UUID contentId) {}

  private record SubscriptionKey(String sessionId, String subscriptionId) {}
}
