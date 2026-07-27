package com.sb10.mopl.content.repository;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {

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

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE contents c
          SET review_count = COALESCE(agg.cnt, 0),
              average_rating = COALESCE(agg.avg_rating, 0.0)
          FROM contents c2
          LEFT JOIN (
              SELECT cr.content_id,
                     COUNT(cr.id) AS cnt,
                     ROUND(AVG(cr.rating), 1) AS avg_rating
              FROM content_reviews cr
              GROUP BY cr.content_id
          ) agg ON c2.id = agg.content_id
          WHERE c.id = c2.id
            AND (c.review_count <> COALESCE(agg.cnt, 0)
                OR c.average_rating <> COALESCE(agg.avg_rating, 0.0))
          """,
      nativeQuery = true)
  int syncReviewStatistics();

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE contents c
          SET watcher_count = COALESCE(agg.cnt, 0)
          FROM contents c2
          LEFT JOIN (
              SELECT ws.content_id,
                     COUNT(ws.id) AS cnt
              FROM watching_session ws
              GROUP BY ws.content_id
          ) agg ON c2.id = agg.content_id
          WHERE c.id = c2.id
            AND c.watcher_count <> COALESCE(agg.cnt, 0)
          """,
      nativeQuery = true)
  int syncWatcherCount();
}
