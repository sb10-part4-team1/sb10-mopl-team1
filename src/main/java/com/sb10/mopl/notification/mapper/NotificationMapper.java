package com.sb10.mopl.notification.mapper;

import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "receiverId", source = "user.id")
  NotificationDto toDto(Notification notification);
}
