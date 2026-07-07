package com.sb10.mopl.conversation.dto;

import com.sb10.mopl.user.dto.response.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content) {}
