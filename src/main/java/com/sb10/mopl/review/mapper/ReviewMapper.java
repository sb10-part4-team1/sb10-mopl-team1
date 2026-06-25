package com.sb10.mopl.review.mapper;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

  @Mapping(source = "targetContent", target = "targetContent")
  Review toEntity(ReviewCreateRequest request, Content targetContent, User user);

  @Mapping(source = "targetContent.id", target = "contentId")
  @Mapping(source = "user.id", target = "author.userId")
  @Mapping(source = "user.name", target = "author.name")
  @Mapping(source = "user.profileImageUrl", target = "author.profileImageUrl")
  @Mapping(source = "content", target = "text")
  ReviewDto toDto(Review review);
}
