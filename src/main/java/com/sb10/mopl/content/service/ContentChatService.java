package com.sb10.mopl.content.service;

import com.sb10.mopl.content.dto.ContentChatDto;
import com.sb10.mopl.content.dto.ContentChatSendRequest;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentChatService {

  private final ContentRepository contentRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  // 채팅 dto 생성
  public ContentChatDto sendMessage(UUID senderId, UUID contentId, ContentChatSendRequest request) {
    if (!contentRepository.existsById(contentId)) {
      throw new ContentException(
        ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
    }

    User sender =
      userRepository
        .findById(senderId)
        .orElseThrow(
          () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", senderId)));

    return new ContentChatDto(userMapper.toSummary(sender), request.content());
  }
}
