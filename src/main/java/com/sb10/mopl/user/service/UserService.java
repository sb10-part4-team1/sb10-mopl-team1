package com.sb10.mopl.user.service;

import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.auth.service.TemporaryPasswordService;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.user.dto.ChangePasswordRequest;
import com.sb10.mopl.user.dto.UserCreateRequest;
import com.sb10.mopl.user.dto.UserDto;
import com.sb10.mopl.user.dto.UserLockUpdateRequest;
import com.sb10.mopl.user.dto.UserRoleUpdateRequest;
import com.sb10.mopl.user.dto.UserSearchRequest;
import com.sb10.mopl.user.dto.UserUpdateRequest;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.event.UserRoleChangedEvent;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final AuthSessionService authSessionService;
  private final TemporaryPasswordService temporaryPasswordService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;
  private final ImageStorageService imageStorageService;

  @Transactional
  public UserDto signUp(UserCreateRequest userCreateRequest) {
    String name = userCreateRequest.name();
    String email = userCreateRequest.email();

    if (userRepository.existsByEmail(email)) {
      throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
    }

    String password = passwordEncoder.encode(userCreateRequest.password());
    User user = User.createUser(name, email, password, null);

    User saved = userRepository.save(user);
    return userMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public UserDto findUser(UUID userId) {
    User user =
        userRepository
            .findByIdAndIsDeletedFalse(userId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));
    return userMapper.toDto(user);
  }

  @Transactional
  public UserDto updateProfile(
      UUID targetUserId,
      UUID requesterUserId,
      UserUpdateRequest userUpdateRequest,
      MultipartFile image) {
    if (!targetUserId.equals(requesterUserId)) {
      throw new UserException(
          UserErrorCode.USER_ACCESS_DENIED,
          Map.of("userId", targetUserId, "requesterId", requesterUserId));
    }

    User user =
        userRepository
            .findByIdAndIsDeletedFalse(targetUserId)
            .orElseThrow(
                () ->
                    new UserException(
                        UserErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId)));

    String profileImageUrl = uploadProfileImageOrKeep(image, user.getProfileImageUrl());
    user.updateProfile(userUpdateRequest.name(), profileImageUrl);

    return userMapper.toDto(user);
  }

  @Transactional
  public void changePassword(
      UUID targetUserId, UUID requesterUserId, ChangePasswordRequest changePasswordRequest) {
    if (!targetUserId.equals(requesterUserId)) {
      throw new UserException(
          UserErrorCode.USER_ACCESS_DENIED,
          Map.of("userId", targetUserId, "requesterId", requesterUserId));
    }

    User user =
        userRepository
            .findByIdAndIsDeletedFalse(targetUserId)
            .orElseThrow(
                () ->
                    new UserException(
                        UserErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId)));

    String encodedPassword = passwordEncoder.encode(changePasswordRequest.password());
    user.changePassword(encodedPassword);
    temporaryPasswordService.deleteByUserId(targetUserId);
    authSessionService.invalidateAllByUserId(targetUserId);
  }

  @Transactional
  public void updateRole(
      UUID targetUserId, UUID changedByUserId, UserRoleUpdateRequest userRoleUpdateRequest) {
    User user =
        userRepository
            .findByIdAndIsDeletedFalse(targetUserId)
            .orElseThrow(
                () ->
                    new UserException(
                        UserErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId)));

    UserRole previousRole = user.getRole();
    UserRole newRole = userRoleUpdateRequest.role();

    if (previousRole == newRole) {
      return;
    }

    user.changeRole(newRole);
    authSessionService.invalidateAllByUserId(targetUserId);
    eventPublisher.publishEvent(
        new UserRoleChangedEvent(
            targetUserId, previousRole, newRole, changedByUserId, clock.instant()));
  }

  @Transactional
  public void updateLocked(UUID targetUserId, UserLockUpdateRequest userLockUpdateRequest) {
    User user =
        userRepository
            .findByIdAndIsDeletedFalse(targetUserId)
            .orElseThrow(
                () ->
                    new UserException(
                        UserErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId)));

    boolean locked = userLockUpdateRequest.locked();
    user.changeLocked(locked);

    if (locked) {
      authSessionService.invalidateAllByUserId(targetUserId);
    }
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<UserDto> findUsers(UserSearchRequest request) {
    validateFindUsersRequest(request);

    List<User> users = userRepository.findAllByCondition(request);
    boolean hasNext = users.size() > request.limit();
    List<User> pageUsers = hasNext ? users.subList(0, request.limit()) : users;
    List<UserDto> data = pageUsers.stream().map(userMapper::toDto).toList();
    User lastUser = hasNext && !pageUsers.isEmpty() ? pageUsers.get(pageUsers.size() - 1) : null;
    long totalCount = userRepository.countByCondition(request);

    return new CursorPageResponse<>(
        data,
        getNextCursor(lastUser, request.sortBy()),
        getNextIdAfter(lastUser),
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection());
  }

  private void validateFindUsersRequest(UserSearchRequest request) {
    if (request == null) {
      throw new UserException(UserErrorCode.INVALID_USER_VALUE, Map.of("request", "요청 값은 필수입니다."));
    }

    boolean hasCursor = request.cursor() != null && !request.cursor().isBlank();
    boolean hasIdAfter = request.idAfter() != null;
    if (hasCursor != hasIdAfter) {
      throw new UserException(
          UserErrorCode.INVALID_USER_VALUE, Map.of("cursor", "cursor와 idAfter는 함께 전달되어야 합니다."));
    }
  }

  private String getNextCursor(User lastUser, UserSearchRequest.SortBy sortBy) {
    if (lastUser == null) {
      return null;
    }

    return switch (sortBy) {
      case name -> lastUser.getName();
      case email -> lastUser.getEmail();
      case createdAt -> lastUser.getCreatedAt().toString();
      case isLocked -> Boolean.toString(lastUser.isLocked());
      case role -> lastUser.getRole().name();
    };
  }

  private UUID getNextIdAfter(User lastUser) {
    return lastUser == null ? null : lastUser.getId();
  }

  private String uploadProfileImageOrKeep(MultipartFile image, String currentProfileImageUrl) {
    if (image == null || image.isEmpty()) {
      return currentProfileImageUrl;
    }

    String uploadedUrl = imageStorageService.upload(image);
    return uploadedUrl == null || uploadedUrl.isBlank() ? currentProfileImageUrl : uploadedUrl;
  }
}
