package com.sb10.mopl.sse.controller.api;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseControllerApiDocs {

  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content =
            @Content(
                mediaType = "text/event-stream",
                schema = @Schema(implementation = SseEmitter.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  SseEmitter subscribe(@Parameter(hidden = true) AuthenticatedUser currentUser, UUID lastEventId);
}
