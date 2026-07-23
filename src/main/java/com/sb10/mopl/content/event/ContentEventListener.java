package com.sb10.mopl.content.event;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentDocument;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.ContentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEventListener {

  private final ContentRepository contentRepository;
  private final ContentSearchRepository contentSearchRepository;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleSpringEvent(ContentEvent event) {
    log.info("[Spring 이벤트 수신 - DB 커밋 완료 후 ES 색인 진행] Content ID: {}", event.contentId());
    processEsSync(event);
  }

  @KafkaListener(topics = "content-sync-events", groupId = "mopl-es-sync-group")
  public void handleKafkaEvent(ContentEvent event) {
    log.info("[Kafka 분산 이벤트 수신 - ES 색인 진행] Content ID: {}", event.contentId());
    processEsSync(event);
  }

  private void processEsSync(ContentEvent event) {
    try {
      if (event.eventType() == ContentEvent.ContentEventType.DELETED) {
        contentSearchRepository.deleteById(event.contentId().toString());
        log.info("[ES 동기화] Content 삭제 반영 완료: {}", event.contentId());
        return;
      }

      Content content = contentRepository.findById(event.contentId()).orElse(null);
      if (content != null) {
        ContentDocument document = ContentDocument.from(content);
        contentSearchRepository.save(document);
        log.info("[ES 동기화] Content 저장/수정 반영 완료: {}", event.contentId());
      }
    } catch (Exception e) {
      log.error("[ES 동기화 처리 실패] Content ID: {}", event.contentId(), e);
    }
  }
}
