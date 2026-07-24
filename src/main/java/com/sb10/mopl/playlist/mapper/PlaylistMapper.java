package com.sb10.mopl.playlist.mapper;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.playlist.dto.PlaylistContentSummaryDto;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.user.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

  default Playlist toEntity(User owner, PlaylistCreateRequest request) {
    return new Playlist(owner, request.title(), request.description());
  }

  @Mapping(source = "playlist.owner.id", target = "owner.userId")
  @Mapping(source = "playlist.owner.name", target = "owner.name")
  @Mapping(source = "playlist.owner.profileImageUrl", target = "owner.profileImageUrl")
  @Mapping(source = "subscriberCount", target = "subscriberCount")
  @Mapping(source = "subscribedByMe", target = "subscribedByMe")
  @Mapping(source = "contents", target = "contents")
  PlaylistDto toDto(
      Playlist playlist,
      long subscriberCount,
      boolean subscribedByMe,
      List<PlaylistContentSummaryDto> contents);

  default PlaylistContentSummaryDto toContentSummaryDto(Content content) {
    return new PlaylistContentSummaryDto(
        content.getId(),
        toApiType(content.getType()),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        toTagNames(content.getContentTags()),
        content.getAverageRating(),
        content.getReviewCount());
  }

  default List<String> toTagNames(List<ContentTag> contentTags) {
    if (contentTags == null || contentTags.isEmpty()) {
      return List.of();
    }

    return contentTags.stream().map(contentTag -> contentTag.getTag().getName()).toList();
  }

  default String toApiType(ContentType type) {
    if (type == null) {
      return null;
    }

    String[] words = type.name().toLowerCase().split("_");
    StringBuilder result = new StringBuilder(words[0]);

    for (int i = 1; i < words.length; i++) {
      String word = words[i];

      result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }

    return result.toString();
  }
}
