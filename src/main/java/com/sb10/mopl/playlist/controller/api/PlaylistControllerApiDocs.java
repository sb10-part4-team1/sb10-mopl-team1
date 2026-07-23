package com.sb10.mopl.playlist.controller.api;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.common.exception.ErrorResponse;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.dto.PlaylistUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "플레이리스트 관리")
public interface PlaylistControllerApiDocs {

  @Operation(summary = "플레이리스트 생성", description = "생성한 플레이리스트는 API 요청자 본인의 플레이리스트로 생성됩니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "성공",
        content = @Content(schema = @Schema(implementation = PlaylistDto.class))),
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
  @SecurityRequirement(name = "CsrfToken")
  ResponseEntity<PlaylistDto> create(
      PlaylistCreateRequest request, @Parameter(hidden = true) AuthenticatedUser currentUser);

  @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)")
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
  ResponseEntity<CursorPageResponse<PlaylistDto>> findAll(
      @Parameter(description = "검색 키워드") String keywordLike,
      @Parameter(description = "소유자 ID") UUID ownerIdEqual,
      @Parameter(description = "구독자 ID") UUID subscriberIdEqual,
      @Parameter(description = "커서") String cursor,
      @Parameter(description = "보조 커서") UUID idAfter,
      @Parameter(description = "한 번에 가져올 개수") Integer limit,
      @Parameter(description = "정렬 기준") String sortBy,
      @Parameter(description = "정렬 방향") SortDirection sortDirection,
      @Parameter(hidden = true) AuthenticatedUser currentUser);

  @Operation(summary = "플레이리스트 단건 조회")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = PlaylistDto.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<PlaylistDto> findById(
      @Parameter(description = "플레이리스트 ID") UUID playlistId,
      @Parameter(hidden = true) AuthenticatedUser currentUser);

  @Operation(summary = "플레이리스트 수정", description = "플레이리스트 소유자만 수정할 수 있습니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = PlaylistDto.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "권한 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  @SecurityRequirement(name = "CsrfToken")
  ResponseEntity<PlaylistDto> update(
      @Parameter(description = "플레이리스트 ID") UUID playlistId,
      PlaylistUpdateRequest request,
      @Parameter(hidden = true) AuthenticatedUser currentUser);

  @Operation(summary = "플레이리스트 삭제", description = "플레이리스트 소유자만 삭제할 수 있습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "권한 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  @SecurityRequirement(name = "CsrfToken")
  ResponseEntity<Void> delete(
      @Parameter(description = "플레이리스트 ID") UUID playlistId,
      @Parameter(hidden = true) AuthenticatedUser currentUser);
}
