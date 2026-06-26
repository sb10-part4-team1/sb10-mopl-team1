package com.sb10.mopl.content.repository;

import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.entity.Content;
import java.util.List;

public interface ContentRepositoryCustom {
  List<Content> findAllByCondition(ContentSearchRequest request);

  long countContents(ContentSearchRequest request);
}
