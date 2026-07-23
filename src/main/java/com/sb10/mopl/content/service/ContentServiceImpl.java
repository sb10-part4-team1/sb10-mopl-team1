package com.sb10.mopl.content.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentSearchRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
// @Primary
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentMapper contentMapper;
  private final ImageStorageService imageStorageService;

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

    // 순수 RDB QueryDSL 대조군 서비스
    List<Content> contents = contentRepository.findAllByCondition(request);
    long totalCount = contentRepository.countContents(request);

    boolean hasNext = contents.size() > limit;
    List<Content> resultContents = hasNext ? contents.subList(0, limit) : contents;
    List<ContentDto> dtos = resultContents.stream().map(contentMapper::toDto).toList();

    UUID nextIdAfter = (hasNext && !dtos.isEmpty()) ? dtos.get(dtos.size() - 1).id() : null;
    String nextCursor = null;
    if (hasNext && !resultContents.isEmpty()) {
      Content lastContent = resultContents.get(resultContents.size() - 1);
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
        totalCount,
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
}
