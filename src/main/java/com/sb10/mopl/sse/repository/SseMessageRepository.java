package com.sb10.mopl.sse.repository;

import com.sb10.mopl.sse.SseMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class SseMessageRepository {

  private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
  private final LinkedHashMap<UUID, SseMessage> messages = new LinkedHashMap<>();

  @Value("${sse.event-queue-capacity:100}")
  private int eventQueueCapacity;

  public SseMessage save(SseMessage message) {
    makeAvailableCapacity();

    UUID eventId = message.getEventId();

    messages.put(eventId, message);

    return message;
  }

  public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID eventId, UUID receiverId) {
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
  }

  private void makeAvailableCapacity() {
    while (messages.size() >= eventQueueCapacity) {

      Iterator<Entry<UUID, SseMessage>> iterator =
        messages.entrySet().iterator();

      if (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    }
  }
}
