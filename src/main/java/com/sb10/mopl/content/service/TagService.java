package com.sb10.mopl.content.service;

import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

  private final TagRepository tagRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<Tag> getOrCreateTags(Set<String> normalizedNames) {
    // 1. 요청된 태그명 중 이미 DB에 등록되어 있는 태그들을 먼저 조회합니다.
    List<Tag> existingTags = tagRepository.findAllByNameIn(normalizedNames);
    Set<String> existingNames = existingTags.stream().map(Tag::getName).collect(Collectors.toSet());

    // 2. 이미 존재하는 태그를 제외하고, DB에 새로 등록해야 하는 신규 태그들만 필터링합니다.
    List<Tag> newTags =
        normalizedNames.stream()
            .filter(name -> !existingNames.contains(name))
            .map(Tag::create)
            .toList();

    // 3. 신규 태그가 존재할 경우 DB에 일괄 저장(saveAll)합니다.
    // 만약 동시 요청으로 인해 타 트랜잭션이 먼저 인서트했다면 이 시점에 Unique 제약 조건 예외가 터지며 트랜잭션이 롤백됩니다.
    if (!newTags.isEmpty()) {
      List<Tag> savedNewTags = tagRepository.saveAll(newTags);
      List<Tag> result = new ArrayList<>(existingTags);
      result.addAll(savedNewTags);
      return result;
    }

    return existingTags;
  }
}
