package com.sb10.mopl.notification.repository;

import com.sb10.mopl.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository
    extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

  long countByUserIdAndIsReadFalse(UUID userId);
}
