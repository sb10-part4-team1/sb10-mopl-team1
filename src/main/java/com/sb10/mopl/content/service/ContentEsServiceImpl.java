package com.sb10.mopl.content.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.dto.EsSearchResult;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.event.ContentEvent;
import com.sb10.mopl.content.event.ContentEventPublisher;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.mapper.ContentMapper;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.ContentSearchRepository;
import com.sb10.mopl.content.repository.TagRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentEsServiceImpl implements ContentService {

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentMapper contentMapper;
  private final ImageStorageService imageStorageService;
  private final ContentSearchRepository contentSearchRepository;
  private final ContentEventPublisher contentEventPublisher;

  @Value("${mopl.default-image-url}")
  private String defaultImageUrl;

  @Override
  @Transactional
  public ContentDto create(ContentCreateRequest request, MultipartFile thumbnail) {
    String thumbnailUrl =
        thumbnail != null && !thumbnail.isEmpty()
            ? uploadThumbnailOrKeep(thumbnail, defaultImageUrl)
            : defaultImageUrl;

    Content content =
        Content.create(request.title(), request.type(), request.description(), thumbnailUrl);

    saveContentTags(content, request.tags());
    Content savedContent = contentRepository.save(content);

    syncToElasticsearch(savedContent, ContentEvent.ContentEventType.CREATED);

    return contentMapper.toDto(savedContent);
  }

  @Override
  @Transactional
  public ContentDto update(UUID id, ContentUpdateRequest request, MultipartFile thumbnail) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));

    String thumbnailUrl = uploadThumbnailOrKeep(thumbnail, content.getThumbnailUrl());

    content.update(request.title(), request.description(), thumbnailUrl);

    if (request.tags() != null) {
      content.getContentTags().clear();
      contentRepository.flush();
      saveContentTags(content, request.tags());
    }

    syncToElasticsearch(content, ContentEvent.ContentEventType.UPDATED);

    return contentMapper.toDto(content);
  }

  @Override
  @Transactional
  public ContentDto updateStatistics(UUID id, double averageRating, int reviewCount) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));

    content.updateStatistics(averageRating, reviewCount);
    syncToElasticsearch(content, ContentEvent.ContentEventType.UPDATED);

    return contentMapper.toDto(content);
  }

  @Override
  @Transactional
  public ContentDto updateWatcherCount(UUID id, long watcherCount) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));

    content.updateWatcherCount(watcherCount);
    syncToElasticsearch(content, ContentEvent.ContentEventType.UPDATED);

    return contentMapper.toDto(content);
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));

    contentRepository.delete(content);

    deleteFromElasticsearch(id);
  }

  @Override
  public ContentDto find(UUID id) {
    Content content =
        contentRepository
            .findById(id)
            .orElseThrow(
                () -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("id", id)));
    return contentMapper.toDto(content);
  }

  @Override
  public CursorPageResponse<ContentDto> findAll(ContentSearchRequest request) {
    int limit = request.limit() != null ? request.limit() : 20;

    // ES 2-Phase 검색: 쿼리 1회로 totalHits (총 개수) + 정렬된 상위 N개 ID 추출 (0.001초!)
    EsSearchResult esResult = contentSearchRepository.search(request, limit + 1);
    List<UUID> esIds = esResult.ids();

    boolean hasNext = esIds.size() > limit;
    List<UUID> targetIds = hasNext ? esIds.subList(0, limit) : esIds;

    // RDB에서는 정렬/조인/Count 쿼리 0회, 단지 ID로 단건 Fetch만 수행!
    List<Content> rawContents =
        targetIds.isEmpty() ? List.of() : contentRepository.findAllByIdIn(targetIds);

    // ES가 정렬해 준 순서 보장 (Map 기반 순서 매핑)
    Map<UUID, Content> contentMap =
        rawContents.stream().collect(Collectors.toMap(Content::getId, Function.identity()));

    List<Content> contents =
        targetIds.stream().map(contentMap::get).filter(c -> c != null).toList();

    List<ContentDto> dtos = contents.stream().map(contentMapper::toDto).toList();

    UUID nextIdAfter = (hasNext && !dtos.isEmpty()) ? dtos.get(dtos.size() - 1).id() : null;
    String nextCursor = null;
    if (hasNext && !contents.isEmpty()) {
      Content lastContent = contents.get(contents.size() - 1);
      nextCursor =
          switch (request.sortBy()) {
            case watcherCount -> lastContent.getWatcherCount() + "_" + lastContent.getReviewCount();
            case createdAt -> lastContent.getCreatedAt().toString();
            case rate -> String.valueOf(lastContent.getAverageRating());
          };
    }

    return new CursorPageResponse<>(
        dtos,
        nextCursor,
        nextIdAfter,
        hasNext,
        esResult.totalHits(),
        request.sortBy().name(),
        request.sortDirection());
  }

  private String uploadThumbnailOrKeep(MultipartFile thumbnail, String currentThumbnailUrl) {
    if (thumbnail == null || thumbnail.isEmpty()) {
      return currentThumbnailUrl;
    }

    String uploadedUrl = imageStorageService.upload(thumbnail);

    return uploadedUrl != null && !uploadedUrl.isBlank() ? uploadedUrl : currentThumbnailUrl;
  }

  private void saveContentTags(Content content, List<String> tagNames) {
    if (tagNames != null && !tagNames.isEmpty()) {
      Set<String> normalizedNames = normalizeTagNames(tagNames);

      List<Tag> existingTags = tagRepository.findAllByNameIn(normalizedNames);
      Set<String> existingTagNames =
          existingTags.stream().map(Tag::getName).collect(Collectors.toSet());

      List<Tag> newTags =
          normalizedNames.stream()
              .filter(name -> !existingTagNames.contains(name))
              .map(Tag::create)
              .toList();

      List<Tag> savedNewTags;
      try {
        savedNewTags = newTags.isEmpty() ? List.of() : tagRepository.saveAll(newTags);
      } catch (DataIntegrityViolationException e) {
        throw new ContentException(
            ContentErrorCode.DUPLICATE_TAG_NAME,
            Map.of("tags", newTags.stream().map(Tag::getName).toList()),
            e);
      }

      List<Tag> tagsToMap = new ArrayList<>(existingTags);
      tagsToMap.addAll(savedNewTags);

      tagsToMap.forEach(tag -> ContentTag.create(content, tag));
    }
  }

  private LinkedHashSet<String> normalizeTagNames(List<String> tagNames) {
    return tagNames.stream()
        .filter(tagName -> tagName != null && !tagName.isBlank())
        .map(String::trim)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void syncToElasticsearch(Content content, ContentEvent.ContentEventType eventType) {
    try {
      contentEventPublisher.publish(new ContentEvent(content.getId(), eventType));
    } catch (Exception ignored) {
      // 이벤트 발행 실패 시 RDB 트랜잭션 보호
    }
  }

  private void deleteFromElasticsearch(UUID id) {
    try {
      contentEventPublisher.publish(new ContentEvent(id, ContentEvent.ContentEventType.DELETED));
    } catch (Exception ignored) {
      // 이벤트 발행 실패 시 RDB 트랜잭션 보호
    }
  }
}
