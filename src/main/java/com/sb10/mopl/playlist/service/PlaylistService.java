package com.sb10.mopl.playlist.service;

import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import java.util.UUID;

public interface PlaylistService {

  PlaylistDto create(PlaylistCreateRequest request, UUID ownerId);
}
