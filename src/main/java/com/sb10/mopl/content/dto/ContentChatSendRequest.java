package com.sb10.mopl.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentChatSendRequest(
    @NotBlank(message = "메시지 내용은 필수입니다.") @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다.")
        String content) {}
