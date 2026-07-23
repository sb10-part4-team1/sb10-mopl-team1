package com.sb10.mopl.notification.controller;

import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.auth.security.principal.CurrentUser;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.notification.controller.api.NotificationControllerApiDocs;
import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.dto.NotificationSearchRequest;
import com.sb10.mopl.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerApiDocs {

  private final NotificationService notificationService;

  // 내 알림 목록 조회
  @Override
  @GetMapping
  public ResponseEntity<CursorPageResponse<NotificationDto>> find(
      @CurrentUser AuthenticatedUser currentUser,
      @ModelAttribute @Valid NotificationSearchRequest request) {
    CursorPageResponse<NotificationDto> response =
        notificationService.findByReceiver(currentUser.id(), request);
    return ResponseEntity.ok(response);
  }

  // 알림 읽음 처리
  @Override
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID notificationId, @CurrentUser AuthenticatedUser currentUser) {
    notificationService.markAsRead(notificationId, currentUser.id());
    return ResponseEntity.noContent().build();
  }
}
