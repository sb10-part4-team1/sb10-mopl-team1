package com.sb10.mopl.watchingsession.controller.api;

import com.sb10.mopl.common.exception.ErrorResponse;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "시청 세션 관리")
public interface WatchingSessionControllerApiDocs {

  @Operation(summary = "특정 사용자의 시청 세션 조회 (nullable)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = WatchingSessionDto.class))),
    @ApiResponse(responseCode = "204", description = "시청 세션 없음", content = @Content),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<WatchingSessionDto> findWatchingSession(
      @Parameter(description = "시청자 ID") UUID watcherId);

  @Operation(summary = "특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<CursorPageResponse<WatchingSessionDto>> findWatchingSessions(
      @Parameter(description = "콘텐츠 ID") UUID contentId,
      @ParameterObject WatchingSessionSearchRequest request);
}
