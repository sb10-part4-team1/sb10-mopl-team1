package com.sb10.mopl.content.repository;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {

  @EntityGraph(attributePaths = {"contentTags", "contentTags.tag"})
  List<Content> findAllByIdIn(List<UUID> ids);

  List<Content> findByProviderAndProviderIdIn(ContentProvider provider, List<String> providerIds);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      UPDATE Content c
      SET c.watcherCount = c.watcherCount + 1
      WHERE c.id = :contentId
      """)
  void incrementWatcherCount(@Param("contentId") UUID contentId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      UPDATE Content c
      SET c.watcherCount = c.watcherCount - 1
      WHERE c.id = :contentId AND c.watcherCount > 0
      """)
  void decrementWatcherCount(@Param("contentId") UUID contentId);
}
