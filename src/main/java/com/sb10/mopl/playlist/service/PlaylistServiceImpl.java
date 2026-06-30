package com.sb10.mopl.playlist.service;

import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.mapper.PlaylistMapper;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistMapper playlistMapper;

  @Override
  @Transactional
  public PlaylistDto create(PlaylistCreateRequest request, UUID ownerId) {
    // 플레이리스트 소유자 조회
    User owner =
        userRepository
            .findById(ownerId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("ownerId", ownerId)));

    // 요청 DTO를 엔티티로 변환
    Playlist playlist = playlistMapper.toEntity(owner, request);

    // 저장 후 응답 DTO로 변환
    Playlist savedPlaylist = playlistRepository.save(playlist);

    return playlistMapper.toDto(savedPlaylist);
  }
}
