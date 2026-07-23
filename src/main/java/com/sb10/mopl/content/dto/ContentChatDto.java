package com.sb10.mopl.content.dto;

import com.sb10.mopl.user.dto.UserSummary;

public record ContentChatDto(UserSummary sender, String content) {}
