package com.sb10.mopl.content.mapper;

import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ContentMapper {

  @Mapping(target = "tags", source = "contentTags", qualifiedByName = "mapContentTagsToStrings")
  ContentDto toDto(Content content);

  @Named("mapContentTagsToStrings")
  default List<String> mapContentTagsToStrings(List<ContentTag> contentTags) {
    if (contentTags == null) {
      return List.of();
    }
    return contentTags.stream().map(contentTag -> contentTag.getTag().getName()).toList();
  }
}
