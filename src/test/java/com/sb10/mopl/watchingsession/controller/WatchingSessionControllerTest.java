package com.sb10.mopl.watchingsession.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.content.dto.ContentSummary;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.user.dto.UserSummary;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = WatchingSessionController.class,
    excludeAutoConfiguration = {
      OAuth2ClientWebSecurityAutoConfiguration.class,
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class
    })
@Import({GlobalExceptionHandler.class})
class WatchingSessionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WatchingSessionService watchingSessionService;

  @Test
  @DisplayName("시청 중인 콘텐츠가 있으면 200 OK와 WatchingSessionDto를 반환한다")
  void findWatchingSession_success_whenActiveSessionExists() throws Exception {
    // given
    UUID watcherId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    WatchingSessionDto dto =
        new WatchingSessionDto(
            sessionId,
            Instant.parse("2026-07-16T00:00:00Z"),
            new UserSummary(watcherId, "test-watcher", null),
            new ContentSummary(
                contentId,
                ContentType.MOVIE,
                "test-content",
                "설명",
                "https://example.com/thumbnail.jpg",
                List.of("action", "thriller"),
                4.5,
                10));

    when(watchingSessionService.findLatestByWatcher(watcherId)).thenReturn(Optional.of(dto));

    // when & then
    mockMvc
        .perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(sessionId.toString()))
        .andExpect(jsonPath("$.createdAt").value("2026-07-16T00:00:00Z"))
        .andExpect(jsonPath("$.watcher.userId").value(watcherId.toString()))
        .andExpect(jsonPath("$.watcher.name").value("test-watcher"))
        .andExpect(jsonPath("$.watcher.profileImageUrl").doesNotExist())
        .andExpect(jsonPath("$.content.id").value(contentId.toString()))
        .andExpect(jsonPath("$.content.type").value("movie"))
        .andExpect(jsonPath("$.content.title").value("test-content"))
        .andExpect(jsonPath("$.content.averageRating").value(4.5))
        .andExpect(jsonPath("$.content.reviewCount").value(10));

    verify(watchingSessionService).findLatestByWatcher(watcherId);
  }

  @Test
  @DisplayName("시청 중인 콘텐츠가 없으면 204 No Content를 반환한다")
  void findWatchingSession_success_whenNoActiveSession() throws Exception {
    // given
    UUID watcherId = UUID.randomUUID();
    when(watchingSessionService.findLatestByWatcher(watcherId)).thenReturn(Optional.empty());

    // when & then
    mockMvc
        .perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
        .andExpect(status().isNoContent());

    verify(watchingSessionService).findLatestByWatcher(watcherId);
  }
}
