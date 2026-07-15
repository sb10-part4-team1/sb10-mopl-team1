package com.sb10.mopl.sse.service;

import com.sb10.mopl.sse.SseMessage;
import com.sb10.mopl.sse.repository.SseEmitterRepository;
import com.sb10.mopl.sse.repository.SseMessageRepository;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * 로컬, 테스트, 디폴트 환경 전용 단독 서버(In-memory) SSE 서비스 구현체입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Profile({"local", "test", "default"})
public class LocalSseService implements SseService {

  private final SseEmitterRepository sseEmitterRepository;
  private final SseMessageRepository sseMessageRepository;
  private final long timeout = 60L * 60L * 1000L;

  @Override
  public SseEmitter connect(UUID receiverId, UUID lastEventId) {
    SseEmitter sseEmitter = new SseEmitter(timeout);

    sseEmitter.onCompletion(
        () -> {
          log.debug("sse on onCompletion");
          sseEmitterRepository.remove(receiverId, sseEmitter);
        });
    sseEmitter.onTimeout(
        () -> {
          log.debug("sse on onTimeout");
          sseEmitterRepository.remove(receiverId, sseEmitter);
        });
    sseEmitter.onError(
        (ex) -> {
          log.debug("sse on onError");
          sseEmitterRepository.remove(receiverId, sseEmitter);
        });

    sseEmitterRepository.add(receiverId, sseEmitter);

    Optional.ofNullable(lastEventId)
        .ifPresentOrElse(
            id -> {
              sseMessageRepository
                  .findAllByEventIdAfterAndReceiverId(id, receiverId)
                  .forEach(
                      sseMessage -> {
                        try {
                          sseEmitter.send(sseMessage.toEvent());
                        } catch (IOException e) {
                          log.error(e.getMessage(), e);
                        }
                      });
            },
            () -> {
              if (!ping(sseEmitter)) {
                sseEmitterRepository.remove(receiverId, sseEmitter);
                sseEmitter.completeWithError(new IllegalStateException("sse initial ping failed"));
              }
            });

    return sseEmitter;
  }

  @Override
  public void send(Collection<UUID> receiverIds, String eventName, Object data) {
    SseMessage message = sseMessageRepository.save(SseMessage.create(receiverIds, eventName, data));
    Set<DataWithMediaType> event = message.toEvent();
    sseEmitterRepository
        .findAllByReceiverIdsIn(receiverIds)
        .forEach(
            sseEmitter -> {
              try {
                sseEmitter.send(event);
              } catch (IOException e) {
                log.error(e.getMessage(), e);
              }
            });
  }

  @Override
  public void broadcast(String eventName, Object data) {
    SseMessage message = sseMessageRepository.save(SseMessage.createBroadcast(eventName, data));
    Set<DataWithMediaType> event = message.toEvent();
    sseEmitterRepository
        .findAll()
        .forEach(
            sseEmitter -> {
              try {
                sseEmitter.send(event);
              } catch (IOException e) {
                log.error(e.getMessage(), e);
              }
            });
  }

  @Scheduled(fixedDelay = 1000 * 60 * 30)
  public void cleanUp() {
    sseEmitterRepository.findAll().stream()
        .filter(sseEmitter -> !ping(sseEmitter))
        .forEach(
            sseEmitter -> sseEmitter.completeWithError(new RuntimeException("sse ping failed")));
  }

  private boolean ping(SseEmitter sseEmitter) {
    try {
      sseEmitter.send(SseEmitter.event().name("ping").build());
      return true;
    } catch (IOException e) {
      log.error("Failed to send ping event", e);
      return false;
    }
  }
}
