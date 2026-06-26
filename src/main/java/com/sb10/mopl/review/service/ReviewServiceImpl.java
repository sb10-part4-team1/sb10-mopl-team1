package com.sb10.mopl.review.service;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.review.exception.ReviewErrorCode;
import com.sb10.mopl.review.exception.ReviewException;
import com.sb10.mopl.review.mapper.ReviewMapper;
import com.sb10.mopl.review.repository.ReviewRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final ContentRepository contentRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public ReviewDto create(ReviewCreateRequest request, UUID userId) {
    UUID contentId = request.contentId();

    // 리뷰 대상 콘텐츠 존재 여부 검증
    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(
                () ->
                    new ContentException(
                        ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    // 리뷰 작성자 존재 여부 검증
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    // 이미 해당 콘텐츠에 리뷰를 작성했는지 검증
    if (reviewRepository.existsByTargetContentIdAndUserId(contentId, userId)) {
      throw new ReviewException(
          ReviewErrorCode.REVIEW_ALREADY_EXISTS, Map.of("contentId", contentId, "userId", userId));
    }

    Review review = reviewMapper.toEntity(request, content, user);

    try {
      Review savedReview = reviewRepository.save(review);
      return reviewMapper.toDto(savedReview);
    } catch (DataIntegrityViolationException e) {
      throw new ReviewException(
          ReviewErrorCode.REVIEW_ALREADY_EXISTS,
          Map.of("contentId", contentId, "userId", userId),
          e);
    }
  }
}
