package com.sb10.mopl.content.initializer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentDocument;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.ContentSearchRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchDataInitializer implements CommandLineRunner {

  private static final String INDEX_NAME = "contents";
  private static final int BATCH_SIZE = 5000;

  private final ContentRepository contentRepository;
  private final ContentSearchRepository contentSearchRepository;
  private final EntityManager entityManager;
  private final ElasticsearchClient elasticsearchClient;
  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  @Transactional(readOnly = true)
  public void run(String... args) {
    try {
      long dbCount = contentRepository.count();
      if (dbCount == 0) {
        log.info("[ES 동기화 생략] DB에 콘텐츠 데이터가 존재하지 않습니다.");
        return;
      }

      log.info("[ES 무조건 인덱스 초기화 및 신선 재색인 시작] DB 총 건수: {}개", dbCount);

      // 1. 기존 유령 데이터/구버전 매핑 잔재 싹 삭제 후 최신 매핑 생성
      IndexOperations indexOps = elasticsearchOperations.indexOps(ContentDocument.class);
      if (indexOps.exists()) {
        indexOps.delete();
      }
      indexOps.create();
      indexOps.putMapping();

      // 2. 대량 색인 성능 극대화를 위한 인덱스 옵션 조절 (refresh 비활성화, 레플리카 0)
      setIndexSettings("-1", 0);

      try {
        UUID lastId = null;
        long totalSynced = 0;

        while (true) {
          List<Content> contents = fetchNextBatch(lastId);
          if (contents.isEmpty()) {
            break;
          }

          List<ContentDocument> docs = contents.stream().map(ContentDocument::from).toList();
          contentSearchRepository.saveAll(docs);

          lastId = contents.get(contents.size() - 1).getId();
          totalSynced += contents.size();

          entityManager.clear();
          log.info("[ES 대량 색인 진행 중...] ({}/{})", totalSynced, dbCount);
        }
      } finally {
        // 3. 색인 완료 후 refresh_interval 1s, 레플리카 1 원복
        setIndexSettings("1s", 1);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }

      log.info("[ES 재색인 최종 완료!] 총 {}개 문서 색인 완료", contentSearchRepository.count());

    } catch (Exception e) {
      log.error("[ES 대량 색인 중 오류 발생]", e);
    }
  }

  private List<Content> fetchNextBatch(UUID lastId) {
    if (lastId == null) {
      return entityManager
          .createQuery("SELECT c FROM Content c ORDER BY c.id ASC", Content.class)
          .setMaxResults(BATCH_SIZE)
          .getResultList();
    }
    return entityManager
        .createQuery(
            "SELECT c FROM Content c WHERE c.id > :lastId ORDER BY c.id ASC", Content.class)
        .setParameter("lastId", lastId)
        .setMaxResults(BATCH_SIZE)
        .getResultList();
  }

  private void setIndexSettings(String refreshInterval, int numberOfReplicas) {
    try {
      elasticsearchClient
          .indices()
          .putSettings(
              PutIndicesSettingsRequest.of(
                  r ->
                      r.index(INDEX_NAME)
                          .settings(
                              s ->
                                  s.refreshInterval(t -> t.time(refreshInterval))
                                      .numberOfReplicas(String.valueOf(numberOfReplicas)))));
    } catch (Exception e) {
      log.error("[ES 인덱스 설정 변경 실패]", e);
    }
  }
}
