package com.sb10.mopl.conversation.dto;

import com.sb10.mopl.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    @Schema(description = "메시지 ID") UUID id,
    @Schema(description = "대화 ID") UUID conversationId,
    @Schema(description = "메시지 생성 시간") Instant createdAt,
    @Schema(description = "발신자 정보") UserSummary sender,
    @Schema(description = "수신자 정보") UserSummary receiver,
    @Schema(description = "메시지 내용") String content) {}
