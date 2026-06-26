package com.sb10.mopl.sse.repository;

import com.sb10.mopl.sse.SseMessage;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class SseMessageRepository {

  private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
  private final LinkedHashMap<UUID, SseMessage> messages = new LinkedHashMap<>();

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  @Value("${sse.event-queue-capacity:100}")
  private int eventQueueCapacity;

  public SseMessage save(SseMessage message) {
    writeLock.lock();
    try {
      makeAvailableCapacity();

      UUID eventId = message.getEventId();
      messages.put(eventId, message);

      return message;
    } finally {
      writeLock.unlock();
    }
  }

  public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID eventId, UUID receiverId) {
    readLock.lock();
    try {

      boolean found = (eventId == null);

      List<SseMessage> result = new ArrayList<>();

      for (Map.Entry<UUID, SseMessage> entry : messages.entrySet()) {

        if (!found) {
          if (entry.getKey().equals(eventId)) {
            found = true;
          }
          continue;
        }

        SseMessage message = entry.getValue();

        if (message.isReceivable(receiverId)) {
          result.add(message);
        }
      }

      return result;

    } finally {
      readLock.unlock();
    }
  }

  // eventQueueCapacity가 음수인 경우 메서드를 실행하지 않도록 함
  @PostConstruct
  void validateEventQueueCapacity() {
    if (eventQueueCapacity < 1) {
      throw new IllegalStateException("sse.event-queue-capacity must be positive");
    }
  }

  private void makeAvailableCapacity() {
    while (messages.size() >= eventQueueCapacity) {

      Iterator<Entry<UUID, SseMessage>> iterator = messages.entrySet().iterator();

      if (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    }
  }
}
