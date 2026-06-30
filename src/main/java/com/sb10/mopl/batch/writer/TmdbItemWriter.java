package com.sb10.mopl.batch.writer;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import com.sb10.mopl.content.repository.ContentRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/** TMDB 수집용 스텝의 최종 저장을 처리하는 배치 라이터입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbItemWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;

  @Override
  public void write(Chunk<? extends Content> chunk) {
    // 0. 청크에서 전달받은 원본 데이터 추출
    List<? extends Content> chunkContents = chunk.getItems();
    int totalReceivedCount = chunkContents.size();

    // 1. [청크 내 자체 중복 제거]
    // - TMDB 응답 내부에 동일한 (provider, providerId)를 가진 중복 데이터가 존재할 수 있습니다.
    // - uniqueKeys에 조합 키를 add()하며 처음 들어오는 데이터만 걸러냅니다.
    Set<ProviderKey> uniqueKeys = new HashSet<>();
    List<Content> distinctContents = new ArrayList<>();

    for (Content item : chunkContents) {
      ProviderKey key = new ProviderKey(item.getProvider(), item.getProviderId());
      if (uniqueKeys.add(key)) {
        distinctContents.add(item); // 처음 나타난 조합 키인 경우만 저장 대상 후보 리스트에 추가
      }
    }
    int chunkDuplicateCount = totalReceivedCount - distinctContents.size();

    // 2. [DB 존재 여부 일괄 조회를 통한 N+1 제거]
    // - 추출된 providerId 목록으로 DB에 이미 적재된 TMDB 데이터를 단 1번의 IN 쿼리로 일괄 조회합니다.
    List<String> providerIds = distinctContents.stream().map(Content::getProviderId).toList();
    Set<ProviderKey> dbExistingKeys =
        contentRepository.findByProviderAndProviderIdIn(ContentProvider.TMDB, providerIds).stream()
            .map(c -> new ProviderKey(c.getProvider(), c.getProviderId()))
            .collect(Collectors.toSet());

    // 3. [최종 저장 대상 필터링 및 중복 통계 계산]
    // - 청크 내 중복이 제거된 데이터들 중, DB에 존재하지 않는 신규 건만 걸러냅니다.
    List<Content> contentsToSave = new ArrayList<>();
    for (Content item : distinctContents) {
      ProviderKey key = new ProviderKey(item.getProvider(), item.getProviderId());
      if (!dbExistingKeys.contains(key)) {
        contentsToSave.add(item); // DB에 존재하지 않는 신규 데이터만 최종 적재
      }
    }

    // 실제 저장할 건수 및 DB 조회로 인해 중복 제외된 건수 계산
    int savedCount = contentsToSave.size();
    int dbDuplicateCount = distinctContents.size() - savedCount;

    // 4. [신규 컨텐츠 일괄 저장]
    if (!contentsToSave.isEmpty()) {
      contentRepository.saveAll(contentsToSave);
    }

    // 5. [단일 통합 로그 출력]
    log.info(
        "TMDB 저장 완료 - 수집: {}, 신규 저장: {}, DB 중복 제외: {}, 청크 내 중복 제외: {}",
        totalReceivedCount,
        savedCount,
        dbDuplicateCount,
        chunkDuplicateCount);
  }

  private record ProviderKey(ContentProvider provider, String providerId) {}
}
