package com.sb10.mopl.user.event;

import com.sb10.mopl.user.entity.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserRoleChangedEvent(
    UUID targetUserId,
    UserRole previousRole,
    UserRole newRole,
    UUID changedByUserId,
    Instant occurredAt) {}
