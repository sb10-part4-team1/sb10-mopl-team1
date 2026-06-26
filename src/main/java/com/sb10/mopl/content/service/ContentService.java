package com.sb10.mopl.content.service;

import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.mapper.ContentMapper;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    String thumbnailUrl = uploadThumbnailOrKeep(thumbnail, DEFAULT_THUMBNAIL_URL);

    Content content =
        Content.create(request.title(), request.type(), request.description(), thumbnailUrl);

    saveContentTags(content, request.tags());

    Content savedContent = contentRepository.save(content);

    return contentMapper.toDto(savedContent);
  }

  @Transactional
  public ContentDto updateContent(UUID id, ContentUpdateRequest request, MultipartFile thumbnail) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));

    String thumbnailUrl = uploadThumbnailOrKeep(thumbnail, content.getThumbnailUrl());

    saveContentTags(content, request.tags());

    content.update(request.title(), request.description(), thumbnailUrl);

    return contentMapper.toDto(content);
  }

  @Transactional
  public ContentDto updateStatistics(UUID id, double averageRating, int reviewCount) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));
    content.updateStatistics(averageRating, reviewCount);
    return contentMapper.toDto(content);
  }

  @Transactional
  public ContentDto updateWatcherCount(UUID id, long watcherCount) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));
    content.updateWatcherCount(watcherCount);
    return contentMapper.toDto(content);
  }

  @Transactional
  public void deleteContent(UUID id) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));
    contentRepository.delete(content);
  }

  private String uploadThumbnailOrKeep(MultipartFile thumbnail, String currentThumbnailUrl) {
    if (thumbnail == null || thumbnail.isEmpty()) {
      return currentThumbnailUrl;
    }

    String uploadedUrl = imageStorageService.upload(thumbnail);

    return uploadedUrl != null && !uploadedUrl.isBlank() ? uploadedUrl : currentThumbnailUrl;
  }

  private void saveContentTags(Content content, List<String> tagNames) {
    if (tagNames == null || tagNames.isEmpty()) {
      content.getContentTags().clear(); // 빈 태그 리스트가 넘어오면 태그 연관관계 삭제
      return;
    }

    Set<String> normalizedNames = normalizeTagNames(tagNames);
    if (normalizedNames.isEmpty()) {
      content
          .getContentTags()
          .clear(); // 정규화 후 빈 태그 리스트라면 태그 연관관계 삭제 ex) " "와 같은 정규화 시 사라지는 태그만 존재하는 경우
      return;
    }

    // 기존 맵핑 중 새 태그 목록에 포함되지 않는 것들은 컬렉션에서 제거 (orphanRemoval 작동)
    content
        .getContentTags()
        .removeIf(contentTag -> !normalizedNames.contains(contentTag.getTag().getName()));

    // 현재 맵핑 상태에서 이미 연결되어 있는 태그명 수집
    Set<String> currentlyMappedNames =
        content.getContentTags().stream()
            .map(contentTag -> contentTag.getTag().getName())
            .collect(Collectors.toSet());

    // 새 태그 목록 중 아직 맵핑되지 않은 태그명만 추출(넘어온 태그 네임 - 현재 연결되어 있는 태그 = 새로 추가해야 할 태그)
    Set<String> unmappedNames =
        normalizedNames.stream()
            .filter(name -> !currentlyMappedNames.contains(name))
            .collect(Collectors.toSet());

    // 새로 추가해야 할 태그 중 이미 DB에 존재하는 태그를 제외하고 DB에 저장
    // existingTags -> 이미 DB에 존재하는 태그들
    // newTags -> 새로운 태그에서 이미 DB에 존재하지 않아 생성해야 하는 태그들
    if (!unmappedNames.isEmpty()) {
      List<Tag> existingTags = tagRepository.findAllByNameIn(unmappedNames);
      Set<String> existingNames =
          existingTags.stream().map(Tag::getName).collect(Collectors.toSet());

      List<Tag> newTags =
          unmappedNames.stream()
              .filter(name -> !existingNames.contains(name))
              .map(Tag::create)
              .toList();

      List<Tag> savedNewTags;
      try {
        savedNewTags =
            newTags.isEmpty()
                ? List.of()
                : tagRepository.saveAll(newTags); // 새로운 태그가 비어 있지 않다면 DB에 저장
      } catch (DataIntegrityViolationException e) {
        throw new ContentException(
            ContentErrorCode.DUPLICATE_TAG_NAME,
            Map.of("tags", newTags.stream().map(Tag::getName).toList()),
            e);
      }

      List<Tag> tagsToMap = new ArrayList<>(existingTags); // DB에 존재하는 태그 추가
      tagsToMap.addAll(savedNewTags); // 새로 추가된 태그 추가

      tagsToMap.forEach(tag -> ContentTag.create(content, tag)); // 태그와 연관관계 생성
    }
  }

  private LinkedHashSet<String> normalizeTagNames(List<String> tagNames) {
    return tagNames.stream()
        .filter(tagName -> tagName != null && !tagName.isBlank())
        .map(String::trim)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
