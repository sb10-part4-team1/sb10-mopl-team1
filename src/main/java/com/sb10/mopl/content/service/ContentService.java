package com.sb10.mopl.content.service;

import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.mapper.ContentMapper;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

  private static final String DEFAULT_THUMBNAIL_URL = "/uploads/default-thumbnail.png";

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentMapper contentMapper;
  private final ImageStorageService imageStorageService;

  @Transactional
  public ContentDto createContent(ContentCreateRequest request, MultipartFile thumbnail) {
    String thumbnailUrl = uploadThumbnailOrDefault(thumbnail);

    Content content =
        Content.create(request.title(), request.type(), request.description(), thumbnailUrl);

    saveContentTags(content, request.tags());

    Content savedContent = contentRepository.save(content);

    return contentMapper.toDto(savedContent);
  }

  private String uploadThumbnailOrDefault(MultipartFile thumbnail) {
    if (thumbnail == null || thumbnail.isEmpty()) {
      return DEFAULT_THUMBNAIL_URL;
    }

    String uploadedUrl = imageStorageService.upload(thumbnail);

    return uploadedUrl != null && !uploadedUrl.isBlank() ? uploadedUrl : DEFAULT_THUMBNAIL_URL;
  }

  private void saveContentTags(Content content, List<String> tagNames) {
    if (tagNames == null || tagNames.isEmpty()) {
      return;
    }

    Set<String> normalizedNames = normalizeTagNames(tagNames);
    if (normalizedNames.isEmpty()) {
      return;
    }

    // 이미 존재하는 태그 벌크 조회
    List<Tag> existingTags = tagRepository.findAllByNameIn(normalizedNames);

    // 기존 태그들 연관관계 맺고, 조회 완료된 태그명은 이름 목록에서 제거
    for (Tag tag : existingTags) {
      ContentTag.create(content, tag);
      normalizedNames.remove(tag.getName());
    }

    // 남은 태그명은 신규 태그이므로 한 번에 생성하여 저장 및 연관관계 매핑
    if (!normalizedNames.isEmpty()) {
      List<Tag> newTags = normalizedNames.stream().map(Tag::create).toList();

      tagRepository.saveAll(newTags).forEach(tag -> ContentTag.create(content, tag));
    }
  }

  // 중복 제거 및 양 끝 공백 제거
  private LinkedHashSet<String> normalizeTagNames(List<String> tagNames) {
    return tagNames.stream()
        .filter(tagName -> tagName != null && !tagName.isBlank())
        .map(String::trim)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
