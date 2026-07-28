package com.sb10.mopl.watchingsession.listener;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.common.realtime.StompFanOutPublisher;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * 콘텐츠 시청 세션(/sub/contents/{contentId}/watch)의 구독 생명주기를 감지해 시청 참여(JOIN)/이탈(LEAVE)을 기록하고 같은 토픽 구독자에게
 * 브로드캐스트합니다.
 *
 * <p>구독(SUBSCRIBE)은 {@link SubscribeMapping}으로 처리합니다. 반환값은 구독을 요청한 클라이언트에게 직접 응답으로
 * 전달되므로, REST 시청 세션 조회와 이 구독이 서로 다른 요청이라 도착 순서를 보장할 수 없더라도 "내가 방금 시청을 시작했다"는 사실은
 * 브로드캐스트 수신 여부와 무관하게 항상 받을 수 있다. 이미 구독 중이던 다른 클라이언트에게는 별도로 브로드캐스트하므로, 방금 구독한 본인은
 * 직접 응답과 브로드캐스트를 중복으로 받을 수 있는데(등록 시점에 따라 달라짐) 둘 다 같은 내용이라 화면 반영은 watcherId 기준으로
 * upsert하면 문제없다.
 *
 * <p>구독 해제(UNSUBSCRIBE)와 연결 종료(DISCONNECT)에는 destination 정보가 없으므로, 구독 시점에 (sessionId,
 * subscriptionId)를 기준으로 어떤 콘텐츠를 보고 있었는지 메모리에 추적해 둡니다. subscriptionId는 하나의 STOMP 연결 내에서만 유일하므로(예: 서로
 * 다른 탭이 각각 "sub-0"을 부여할 수 있음) sessionId 없이는 다른 연결의 구독과 충돌할 수 있습니다.
 *
 * <p>유저는 동시에 하나의 콘텐츠만 시청할 수 있어(watching_session.watcher_id 유니크), 다른 콘텐츠를 구독하면 세션이 이동하며 이전 콘텐츠
 * 구독자에게도 LEAVE가 브로드캐스트됩니다. 같은 (watcherId, contentId)에 대해 남아있는 활성 구독 수를 세어, 새로고침 등으로 옛 연결과 새 연결이 겹치는
 * 구간에도 마지막 구독이 끊길 때만 실제로 시청 세션을 종료합니다.
 *
 * <p>단일 인스턴스 기준 구현이며, 다중 인스턴스로 확장 시 이 추적 상태는 Redis 등 공유 저장소로 옮겨야 합니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ContentWatchSessionEventListener {

  private final WatchingSessionService watchingSessionService;
  private final StompFanOutPublisher stompFanOutPublisher;

  // subscriptionId는 하나의 STOMP 세션(연결) 안에서만 유일하므로, 서로 다른 연결(탭)의 구독을 구분하기 위해
  // sessionId까지 포함한 복합 키로 관리한다.
  private final Map<SubscriptionKey, WatchSubscription> subscriptionsByKey =
      new ConcurrentHashMap<>();

  // handleDisconnect에서 sessionId로 구독을 O(1)에 찾기 위한 보조 인덱스
  private final Map<String, Set<String>> subscriptionIdsBySessionId = new ConcurrentHashMap<>();

  // (watcherId, contentId) 기준으로 추적 중인 구독 키 목록. 이 Set의 크기가 곧 같은 (watcherId, contentId)를
  // 보고 있는 활성 구독(탭/연결) 수이므로, 새로고침 등으로 발생하는 겹침 구간에서 마지막 구독이 끊길 때만 실제로
  // 시청 세션을 종료하는 데 별도 카운터 없이 이 Set만으로 판단한다. 세션이 다른 콘텐츠로 이동하면 이전 콘텐츠에
  // 대해 추적하던 구독이 전부 무효해지므로, 한 번에 정리하기 위한 역인덱스이기도 하다.
  private final Map<WatcherContentKey, Set<SubscriptionKey>> subscriptionKeysByWatcherContent =
      new ConcurrentHashMap<>();

  // 구독 시점에 join()을 수행하고, 결과를 구독을 요청한 클라이언트에게 직접 응답으로 돌려준다.
  @SubscribeMapping("/contents/{contentId}/watch")
  public WatchingSessionChange subscribe(
      @DestinationVariable UUID contentId, Principal principal, Message<?> message) {
    UUID watcherId = resolve(principal);

    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    String subscriptionId = accessor.getSubscriptionId();
    String sessionId = accessor.getSessionId();

    WatchingSessionJoinResult result = watchingSessionService.join(watcherId, contentId);
    track(sessionId, subscriptionId, new WatchSubscription(sessionId, contentId, watcherId));

    if (result.previousSession() != null) {
      UUID previousContentId = result.previousSession().content().id();
      log.info("[WATCH] 시청 이동: {} -> {}, watcherId={}", previousContentId, contentId, watcherId);
      purgeStaleTracking(watcherId, previousContentId);
      broadcast(previousContentId, ChangeType.LEAVE, result.previousSession());
    }

    log.info("[WATCH] 시청 참여: contentId={}, watcherId={}", contentId, watcherId);
    return broadcast(contentId, ChangeType.JOIN, result.session());
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
      SubscriptionKey key = new SubscriptionKey(sessionId, subscriptionId);
      WatchSubscription subscription = subscriptionsByKey.remove(key);
      if (subscription != null) {
        removeSubscriptionKey(subscription.watcherId(), subscription.contentId(), key);
        leave(subscription);
      }
    }
  }

  // subscriptionsByKey, subscriptionIdsBySessionId, subscriptionKeysByWatcherContent(역인덱스)를 함께
  // 갱신합니다.
  private void track(String sessionId, String subscriptionId, WatchSubscription subscription) {
    SubscriptionKey key = new SubscriptionKey(sessionId, subscriptionId);
    subscriptionsByKey.put(key, subscription);
    subscriptionIdsBySessionId
        .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
        .add(subscriptionId);
    subscriptionKeysByWatcherContent
        .computeIfAbsent(
            new WatcherContentKey(subscription.watcherId(), subscription.contentId()),
            k -> ConcurrentHashMap.newKeySet())
        .add(key);
  }

  // 세 인덱스에서도 함께 제거하며, 세션/콘텐츠의 마지막 구독이었다면 해당 항목 자체도 정리합니다.
  private WatchSubscription untrack(String sessionId, String subscriptionId) {
    SubscriptionKey key = new SubscriptionKey(sessionId, subscriptionId);
    WatchSubscription subscription = subscriptionsByKey.remove(key);
    if (subscription != null) {
      subscriptionIdsBySessionId.computeIfPresent(
          sessionId,
          (k, subscriptionIds) -> {
            subscriptionIds.remove(subscriptionId);
            return subscriptionIds.isEmpty() ? null : subscriptionIds;
          });
      removeSubscriptionKey(subscription.watcherId(), subscription.contentId(), key);
    }
    return subscription;
  }

  private void removeSubscriptionKey(UUID watcherId, UUID contentId, SubscriptionKey key) {
    subscriptionKeysByWatcherContent.computeIfPresent(
        new WatcherContentKey(watcherId, contentId),
        (k, keys) -> {
          keys.remove(key);
          return keys.isEmpty() ? null : keys;
        });
  }

  // 세션이 다른 콘텐츠로 이동하면, 이전 콘텐츠에 대해 추적하던 구독은 더 이상 유효하지 않으므로 모두 정리한다.
  // (이 유저의 다른 탭이 이전 콘텐츠를 계속 구독 중이었더라도, 실제 시청 세션은 이미 새 콘텐츠로 옮겨갔으므로 함께 정리 대상이다.)
  private void purgeStaleTracking(UUID watcherId, UUID previousContentId) {
    WatcherContentKey key = new WatcherContentKey(watcherId, previousContentId);
    Set<SubscriptionKey> staleKeys = subscriptionKeysByWatcherContent.remove(key);

    if (staleKeys == null) {
      return;
    }

    for (SubscriptionKey staleKey : staleKeys) {
      subscriptionsByKey.remove(staleKey);
      subscriptionIdsBySessionId.computeIfPresent(
          staleKey.sessionId(),
          (sessionId, subscriptionIds) -> {
            subscriptionIds.remove(staleKey.subscriptionId());
            return subscriptionIds.isEmpty() ? null : subscriptionIds;
          });
    }
  }

  private void leave(WatchSubscription subscription) {
    // 이 시점에는 removeSubscriptionKey()가 이미 이번 구독을 제거한 뒤이므로, 남은 Set 크기가 곧
    // 같은 (watcherId, contentId)를 아직 보고 있는 다른 탭/연결 수다.
    int remaining = activeSubscriptionCount(subscription.watcherId(), subscription.contentId());
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

  private int activeSubscriptionCount(UUID watcherId, UUID contentId) {
    Set<SubscriptionKey> keys =
        subscriptionKeysByWatcherContent.get(new WatcherContentKey(watcherId, contentId));
    return keys == null ? 0 : keys.size();
  }

  // 같은 토픽의 다른 구독자들에게 브로드캐스트하고, 계산한 변경 내용을 반환한다(구독 응답 재사용 목적).
  private WatchingSessionChange broadcast(UUID contentId, ChangeType type, WatchingSessionDto dto) {
    long watcherCount = watchingSessionService.countWatchers(contentId);
    WatchingSessionChange change = new WatchingSessionChange(type, dto, watcherCount);
    stompFanOutPublisher.publish("/sub/contents/" + contentId + "/watch", change);
    return change;
  }

  // CONNECT 시점에 StompChannelInterceptor가 세션에 부여한 Principal에서 시청자를 꺼낸다.
  // 이 시점의 구독은 이미 StompChannelInterceptor의 인증 검사를 통과했으므로 정상적으로는 항상 존재한다.
  private UUID resolve(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser.id();
    }

    log.warn("[WATCH] 인증 정보가 없는 구독 요청입니다.");
    throw new MoplException(AuthErrorCode.AUTHENTICATION_FAILED, Map.of());
  }

  private record WatchSubscription(String sessionId, UUID contentId, UUID watcherId) {}

  private record WatcherContentKey(UUID watcherId, UUID contentId) {}

  private record SubscriptionKey(String sessionId, String subscriptionId) {}
}
