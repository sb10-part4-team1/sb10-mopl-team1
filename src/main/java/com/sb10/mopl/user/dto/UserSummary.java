package com.sb10.mopl.user.dto;

import java.util.UUID;

public record UserSummary(UUID userId, String name, String profileImageUrl) {}
