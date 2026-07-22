package com.sb10.mopl.notification.event;

import com.sb10.mopl.notification.dto.NotificationDto;

public record NotificationCreatedEvent(NotificationDto notification) {}
