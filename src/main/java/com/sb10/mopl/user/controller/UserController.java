package com.sb10.mopl.user.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.user.controller.api.UserControllerApiDocs;
import com.sb10.mopl.user.dto.request.ChangePasswordRequest;
import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.request.UserLockUpdateRequest;
import com.sb10.mopl.user.dto.request.UserRoleUpdateRequest;
import com.sb10.mopl.user.dto.request.UserSearchRequest;
import com.sb10.mopl.user.dto.request.UserUpdateRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerApiDocs {

  private final UserService userService;

  @Override
  @PostMapping
  public ResponseEntity<UserDto> signUp(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    UserDto userDto = userService.signUp(userCreateRequest);
    return ResponseEntity.created(URI.create("/api/users/" + userDto.id())).body(userDto);
  }

  @Override
  @GetMapping("/{userId}")
  public ResponseEntity<UserDto> findUser(@PathVariable UUID userId) {
    UserDto userDto = userService.findUser(userId);
    return ResponseEntity.ok(userDto);
  }

  @Override
  @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserDto> updateProfile(
      @PathVariable UUID userId,
      @RequestPart("request") @Valid UserUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image,
      @CurrentUser AuthenticatedUser currentUser) {
    UserDto userDto = userService.updateProfile(userId, currentUser.id(), request, image);
    return ResponseEntity.ok(userDto);
  }

  @Override
  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable UUID userId,
      @Valid @RequestBody ChangePasswordRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    userService.changePassword(userId, currentUser.id(), request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{userId}/role")
  public ResponseEntity<Void> updateRole(
      @PathVariable UUID userId,
      @Valid @RequestBody UserRoleUpdateRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    userService.updateRole(userId, currentUser.id(), request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{userId}/locked")
  public ResponseEntity<Void> updateLocked(
      @PathVariable UUID userId, @Valid @RequestBody UserLockUpdateRequest request) {
    userService.updateLocked(userId, request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<CursorPageResponse<UserDto>> findUsers(
      @ModelAttribute @Valid UserSearchRequest request) {
    CursorPageResponse<UserDto> response = userService.findUsers(request);
    return ResponseEntity.ok(response);
  }
}
