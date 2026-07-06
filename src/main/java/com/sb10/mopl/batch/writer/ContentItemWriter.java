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

/** 모든 콘텐츠 수집(배치) 스텝에서 공통으로 사용되는 영속화 라이터입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentItemWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;

  /**
   * 청크 단위 저장 시, 청크 내 자체 중복 제거와 N+1 쿼리 문제를 해결하는 메서드입니다.
   *
   * <p>chunkContents: 이번 청크 범위로 유입된 콘텐츠 엔티티 리스트
   *
   * <p>uniqueKeys를 활용하여 청크 내에 동일하게 들어온 중복 데이터를 먼저 걸러냅니다.
   *
   * <p>중복이 제거된 대상들의 ID 목록으로 IN 쿼리를 날려 DB 존재 여부를 한 번에 조회합니다. (N+1 방지)
   *
   * <p>DB에 존재하지 않는 최종 신규 아이템들만 saveAll()을 통해 일괄 저장합니다.
   */
  @Override
  public void write(Chunk<? extends Content> chunk) {
    List<? extends Content> chunkContents = chunk.getItems(); // 이번 청크 단위에 들어온 아이템
    if (chunkContents.isEmpty()) {
      return;
    }

    // 0. 청크 첫 번째 데이터를 통해 제공처(provider) 식별
    ContentProvider provider = chunkContents.get(0).getProvider();
    int totalReceivedCount = chunkContents.size();

    // 1. [청크 내 자체 중복 제거]
    Set<ProviderKey> uniqueKeys = new HashSet<>();
    List<Content> distinctContents = new ArrayList<>();

    for (Content item : chunkContents) {
      ProviderKey key = new ProviderKey(item.getProvider(), item.getProviderId());
      if (uniqueKeys.add(key)) { // Set에 아이템의 키를 삽입하면서 중복이면 false 반환
        distinctContents.add(item); // 중복이 아닌 키라면 아이템을 추가
      }
    }
    int chunkDuplicateCount =
        totalReceivedCount - distinctContents.size(); // 전체 개수 - 중복 제거 된 아이템의 개수

    // 2. [DB 존재 여부 일괄 조회]
    List<String> providerIds =
        distinctContents.stream().map(Content::getProviderId).toList(); // 아이템들의 Provider_Id 추출
    Set<ProviderKey> dbExistingKeys =
        contentRepository
            .findByProviderAndProviderIdIn(provider, providerIds)
            .stream() // 해당 ID에 해당하는 콘텐츠를 DB에서 존재하는지 확인(새롭게 들어온 데이터가 이미 있던 콘텐츠인지 확인)
            .map(c -> new ProviderKey(c.getProvider(), c.getProviderId()))
            .collect(Collectors.toSet());

    // 3. [최종 저장 대상 필터링]
    List<Content> contentsToSave = new ArrayList<>();
    for (Content item : distinctContents) {
      ProviderKey key = new ProviderKey(item.getProvider(), item.getProviderId());
      if (!dbExistingKeys.contains(key)) { // DB에 없는 신규 컨텐츠라면
        contentsToSave.add(item); // 저장할 대상에 추가
      }
    }

    int savedCount = contentsToSave.size();
    int dbDuplicateCount = distinctContents.size() - savedCount;

    // 4. [신규 콘텐츠 일괄 저장]
    if (!contentsToSave.isEmpty()) {
      contentRepository.saveAll(contentsToSave);
    }

    log.info(
        "[{}] 저장 완료 - 수집: {}, 신규 저장: {}, DB 중복 제외: {}, 청크 내 중복 제외: {}",
        provider,
        totalReceivedCount,
        savedCount,
        dbDuplicateCount,
        chunkDuplicateCount);
  }

  private record ProviderKey(ContentProvider provider, String providerId) {}
}
