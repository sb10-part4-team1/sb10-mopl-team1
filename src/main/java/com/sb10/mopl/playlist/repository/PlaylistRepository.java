package com.sb10.mopl.playlist.repository;

import com.sb10.mopl.playlist.entity.Playlist;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {}
