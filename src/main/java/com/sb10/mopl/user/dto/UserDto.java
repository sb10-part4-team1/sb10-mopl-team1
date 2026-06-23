package com.sb10.mopl.user.dto;

import com.sb10.mopl.user.entity.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    Instant createdAt,
    String email,
    String name,
    String profileImageUrl,
    UserRole role,
    Boolean locked) {}
