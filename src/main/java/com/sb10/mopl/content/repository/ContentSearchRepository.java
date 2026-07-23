package com.sb10.mopl.content.repository;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.EsSearchResult;
import com.sb10.mopl.content.entity.ContentDocument;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentSearchRepository {

  private final ElasticsearchOperations elasticsearchOperations;

  public void save(ContentDocument document) {
    elasticsearchOperations.save(document);
  }

  public void saveAll(List<ContentDocument> documents) {
    elasticsearchOperations.save(documents);
  }

  public void deleteById(String id) {
    elasticsearchOperations.delete(id, ContentDocument.class);
  }

  public long count() {
    return elasticsearchOperations.count(NativeQuery.builder().build(), ContentDocument.class);
  }

  public EsSearchResult search(ContentSearchRequest request, int limit) {
    String keyword = request.keywordLike() != null ? request.keywordLike().trim() : "";
    String typeStr = request.typeEqual() != null ? request.typeEqual().name() : null;

    boolean hasType = typeStr != null && !typeStr.isBlank();
    boolean hasKeyword = !keyword.isBlank();

    Sort.Direction direction =
        request.sortDirection() == SortDirection.ASCENDING
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    Sort sort =
        switch (request.sortBy()) {
          case watcherCount ->
              Sort.by(direction, "watcherCount")
                  .and(Sort.by(direction, "reviewCount"))
                  .and(Sort.by(Sort.Direction.ASC, "id"));
          case rate -> Sort.by(direction, "averageRating").and(Sort.by(Sort.Direction.ASC, "id"));
          case createdAt -> Sort.by(direction, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
        };

    NativeQuery query =
        NativeQuery.builder()
            .withQuery(
                q -> {
                  if (!hasType && !hasKeyword) {
                    return q.matchAll(m -> m);
                  }
                  return q.bool(
                      b -> {
                        if (hasType) {
                          b.must(m -> m.term(t -> t.field("type").value(typeStr)));
                        }
                        if (hasKeyword) {
                          b.must(
                              m ->
                                  m.multiMatch(
                                      mm -> mm.fields("title^3", "description").query(keyword)));
                        }
                        return b;
                      });
                })
            .withSort(sort)
            .withMaxResults(limit)
            .build();

    SearchHits<ContentDocument> searchHits =
        elasticsearchOperations.search(query, ContentDocument.class);

    List<UUID> ids =
        searchHits.stream().map(SearchHit::getContent).map(ContentDocument::getUuidId).toList();

    return new EsSearchResult(ids, searchHits.getTotalHits());
  }
}
