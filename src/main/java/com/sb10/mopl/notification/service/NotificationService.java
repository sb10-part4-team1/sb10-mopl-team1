package com.sb10.mopl.notification.service;

import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.entity.Notification;
import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.mapper.NotificationMapper;
import com.sb10.mopl.notification.repository.NotificationRepository;
import com.sb10.mopl.sse.service.SseService;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private static final String NOTIFICATIONS_EVENT_NAME = "notifications";

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationMapper notificationMapper;
  private final SseService sseService;

  // 알림을 저장하고 SSE로 실시간 발송한다. SSE 발송 실패가 저장된 알림까지 롤백시키지 않도록 별도로 처리한다.
  public NotificationDto create(
      UUID receiverId, String title, String content, NotificationLevel level) {
    User receiver =
        userRepository
            .findById(receiverId)
            .orElseThrow(
                () ->
                    new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", receiverId)));

    Notification notification =
        Notification.builder().user(receiver).title(title).content(content).level(level).build();
    notificationRepository.save(notification);

    NotificationDto dto = notificationMapper.toDto(notification);

    try {
      sseService.send(List.of(receiverId), NOTIFICATIONS_EVENT_NAME, dto);
    } catch (RuntimeException exception) {
      log.error(
          "알림 SSE 발송 실패 - receiverId: {}, notificationId: {}, exceptionType: {}",
          receiverId,
          dto.id(),
          exception.getClass().getSimpleName(),
          exception);
    }

    return dto;
  }
}
