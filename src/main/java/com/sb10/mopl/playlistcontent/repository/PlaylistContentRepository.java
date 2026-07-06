package com.sb10.mopl.playlistcontent.repository;

import com.sb10.mopl.playlistcontent.entity.PlaylistContent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

  @Query(
      """
      select count(pc) > 0
      from PlaylistContent pc
      where pc.playlist.id = :playlistId
        and pc.content.id = :contentId
      """)
  boolean existsByPlaylistIdAndContentId(
      @Param("playlistId") UUID playlistId, @Param("contentId") UUID contentId);

  @Query(
      """
      select pc
      from PlaylistContent pc
      where pc.playlist.id = :playlistId
        and pc.content.id = :contentId
      """)
  Optional<PlaylistContent> findByPlaylistIdAndContentId(
      @Param("playlistId") UUID playlistId, @Param("contentId") UUID contentId);
}
