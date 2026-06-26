package com.sb10.mopl.sse.controller;

import com.sb10.mopl.sse.service.SseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

  // 연결 유지 시간(1시간)
  private static final long TIMEOUT = 60L * 60L * 1000L;

  private final SseService sseService;

  //  private final SseService

  // TODO: 추후 인증 로직 추가에 따라 수정 필요
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      //    @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam(value = "LastEventId", required = false) UUID lastEventId) {

    //    UUID userId = userDetails.getUserDto().id();

    return null;
  }
}
