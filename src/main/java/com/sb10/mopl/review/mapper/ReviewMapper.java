package com.sb10.mopl.review.mapper;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.review.dto.ReviewAuthorDto;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

  public Review toEntity(ReviewCreateRequest request, Content targetContent, User user) {
    return new Review(
      targetContent,
      user,
      request.content(),
      request.rating()
    );
  }

  public ReviewDto toDto(Review review) {
    return new ReviewDto(
      review.getId(),
      review.getTargetContent().getId(),
      new ReviewAuthorDto(
        review.getUser().getId(),
        review.getUser().getName(),
        review.getUser().getProfileImageUrl()
      ),
      review.getContent(),
      review.getRating()
    );
  }
  }
