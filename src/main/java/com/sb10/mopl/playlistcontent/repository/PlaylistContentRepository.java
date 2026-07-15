package com.sb10.mopl.playlistcontent.repository;

import com.sb10.mopl.playlistcontent.entity.PlaylistContent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistContentRepository
    extends JpaRepository<PlaylistContent, UUID>, PlaylistContentRepositoryCustom {}
