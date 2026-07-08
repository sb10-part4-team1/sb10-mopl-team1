package com.sb10.mopl.conversation.dto;

import com.sb10.mopl.user.dto.response.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id, UserSummary with, DirectMessageDto lastestMessage, boolean hasUnread) {}
