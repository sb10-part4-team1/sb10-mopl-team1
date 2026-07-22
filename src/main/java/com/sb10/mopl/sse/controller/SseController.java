package com.sb10.mopl.sse.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.sse.controller.api.SseControllerApiDocs;
import com.sb10.mopl.sse.service.SseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController implements SseControllerApiDocs {

  private final SseService sseService;

  @Override
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @CurrentUser AuthenticatedUser currentUser,
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {
    return sseService.connect(currentUser.id(), lastEventId);
  }
}
