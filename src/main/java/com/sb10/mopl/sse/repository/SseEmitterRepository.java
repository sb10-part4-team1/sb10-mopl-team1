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

  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter save(UUID receiverId, SseEmitter emitter) {
    emitters.compute(receiverId, (key, emitters) -> {
      if (emitters == null) {
        return new CopyOnWriteArrayList<>(List.of(emitter));
      } else {
        emitters.add(emitter);
        return emitters;
      }
    });

    return emitter;
  }

  public List<SseEmitter> findAllByReceiverIdsIn(Collection<UUID> receiverIds) {
    return emitters.entrySet().stream()
      .filter(entry -> receiverIds.contains(entry.getKey()))
      .map(Map.Entry::getValue)
      .flatMap(Collection::stream)
      .toList();
  }

  public List<SseEmitter> findAll() {
    return emitters.values().stream()
      .flatMap(Collection::stream)
      .toList();
  }

  public void remove(UUID receiverId, SseEmitter sseEmitter) {
    emitters.computeIfPresent(receiverId, (key, emitters) -> {
      emitters.remove(sseEmitter);
      return emitters.isEmpty() ? null : emitters;
    });
  }

}
