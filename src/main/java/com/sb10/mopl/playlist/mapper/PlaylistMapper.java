package com.sb10.mopl.playlist.mapper;

import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

  default Playlist toEntity(User owner, PlaylistCreateRequest request) {
    return new Playlist(owner, request.title(), request.description());
  }

  @Mapping(source = "owner.id", target = "owner.userId")
  @Mapping(source = "owner.name", target = "owner.name")
  @Mapping(source = "owner.profileImageUrl", target = "owner.profileImageUrl")
  @Mapping(target = "subscriberCount", expression = "java(0L)")
  @Mapping(target = "subscribedByMe", constant = "false")
  @Mapping(target = "contents", expression = "java(java.util.List.of())")
  PlaylistDto toDto(Playlist playlist);
}
