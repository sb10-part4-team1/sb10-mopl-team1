package com.sb10.mopl.notification.repository;

import com.sb10.mopl.notification.dto.NotificationSearchRequest;
import com.sb10.mopl.notification.entity.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {

  List<Notification> search(UUID receiverId, NotificationSearchRequest request);
}
