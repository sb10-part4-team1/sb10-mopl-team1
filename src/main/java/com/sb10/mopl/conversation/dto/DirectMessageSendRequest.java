package com.sb10.mopl.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectMessageSendRequest(
    @NotBlank(message = "메시지 내용은 필수입니다.") String content) {}
