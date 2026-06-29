package com.sb10.mopl.sse.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

  private final Map<UUID, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

  public void add(UUID receiverId, SseEmitter emitter) {
    emitterMap.computeIfAbsent(receiverId, k -> new CopyOnWriteArrayList<>()).add(emitter);
  }

  public List<SseEmitter> findAllByReceiverIdsIn(Collection<UUID> receiverIds) {
    return receiverIds.stream()
        .map(emitterMap::get)
        .filter(java.util.Objects::nonNull)
        .flatMap(Collection::stream)
        .toList();
  }

  public List<SseEmitter> findAll() {
    return emitterMap.values().stream().flatMap(Collection::stream).toList();
  }

  public void remove(UUID receiverId, SseEmitter sseEmitter) {
    emitterMap.computeIfPresent(
        receiverId,
        (key, emitters) -> {
          emitters.remove(sseEmitter);
          return emitters.isEmpty() ? null : emitters;
        });
  }
}
