package com.sb10.mopl.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @Schema(description = "대화 상대 정보 ID") @NotNull(message = "대화 상대 정보 ID는 필수 입니다.")
        UUID withUserId) {}
