package com.sb10.mopl.conversation.dto;

import com.sb10.mopl.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ConversationDto(
    @Schema(description = "대화 ID") UUID id,
    @Schema(description = "대화 상대 정보") UserSummary with,
    @Schema(description = "마지막 메시지 내용") DirectMessageDto lastestMessage,
    @Schema(description = "읽지 않은 메시지 존재 여부") boolean hasUnread) {}
