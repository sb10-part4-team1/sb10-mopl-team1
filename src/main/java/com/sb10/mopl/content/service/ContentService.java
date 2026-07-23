package com.sb10.mopl.content.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentService {

  ContentDto create(ContentCreateRequest request, MultipartFile thumbnail);

  ContentDto update(UUID id, ContentUpdateRequest request, MultipartFile thumbnail);

  ContentDto updateStatistics(UUID id, double averageRating, int reviewCount);

  ContentDto updateWatcherCount(UUID id, long watcherCount);

  void delete(UUID id);

  ContentDto find(UUID id);

  CursorPageResponse<ContentDto> findAll(ContentSearchRequest request);
}
