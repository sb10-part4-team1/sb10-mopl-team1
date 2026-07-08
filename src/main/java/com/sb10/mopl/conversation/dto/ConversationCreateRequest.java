package com.sb10.mopl.conversation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @NotNull(message = "대화 상대 정보 ID는 필수 입니다.") UUID withUserId // 대화 상대 정보 ID
    ) {}
