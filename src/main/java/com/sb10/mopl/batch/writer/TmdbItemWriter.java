package com.sb10.mopl.batch.writer;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.repository.ContentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

// title + type 기준 중복 체크 후 신규 Content만 저장
// MOVIE, TV_SERIES 모두 동일한 Writer 사용
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbItemWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;

  @Override
  public void write(Chunk<? extends Content> chunk) {
    List<? extends Content> items = chunk.getItems();

    List<? extends Content> newContents =
        items.stream()
            .filter(
                content ->
                    !contentRepository.existsByTitleAndType(content.getTitle(), content.getType()))
            .toList();

    if (!newContents.isEmpty()) {
      contentRepository.saveAll(newContents);
    }

    log.info(
        "TMDB 저장 완료 - 전체: {}건 / 신규: {}건 / 중복 스킵: {}건",
        items.size(),
        newContents.size(),
        items.size() - newContents.size());
  }
}
