package com.sb10.mopl.user.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.user.dto.request.ChangePasswordRequest;
import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.request.UserRoleUpdateRequest;
import com.sb10.mopl.user.dto.request.UserSearchRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> signUp(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    UserDto userDto = userService.signUp(userCreateRequest);
    return ResponseEntity.created(URI.create("/api/users/" + userDto.id())).body(userDto);
  }

  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable UUID userId,
      @Valid @RequestBody ChangePasswordRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    userService.changePassword(userId, currentUser.id(), request);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{userId}/role")
  public ResponseEntity<Void> updateRole(
      @PathVariable UUID userId, @Valid @RequestBody UserRoleUpdateRequest request) {
    userService.updateRole(userId, request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponse<UserDto>> findUsers(
      @ModelAttribute @Valid UserSearchRequest request) {
    CursorPageResponse<UserDto> response = userService.findUsers(request);
    return ResponseEntity.ok(response);
  }
}
