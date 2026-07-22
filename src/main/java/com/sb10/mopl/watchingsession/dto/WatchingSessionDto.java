package com.sb10.mopl.watchingsession.dto;

import com.sb10.mopl.content.dto.ContentSummary;
import com.sb10.mopl.user.dto.response.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto(
    @Schema(description = "시청 세션 ID") UUID id,
    @Schema(description = "시청 세션 생성 시간") Instant createdAt,
    @Schema(description = "시청자 정보") UserSummary watcher,
    @Schema(description = "시청 중인 콘텐츠 정보") ContentSummary content) {}
