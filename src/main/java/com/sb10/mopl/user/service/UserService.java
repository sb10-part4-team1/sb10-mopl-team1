package com.sb10.mopl.user.service;

import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.auth.service.TemporaryPasswordService;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.user.dto.request.ChangePasswordRequest;
import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.request.UserRoleUpdateRequest;
import com.sb10.mopl.user.dto.request.UserSearchRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final AuthSessionService authSessionService;
  private final TemporaryPasswordService temporaryPasswordService;

  @Transactional
  public UserDto signUp(UserCreateRequest userCreateRequest) {
    String name = userCreateRequest.name();
    String email = userCreateRequest.email();

    if (userRepository.existsByEmail(email)) {
      throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
    }

    String password = passwordEncoder.encode(userCreateRequest.password());
    User user = User.createUser(name, email, password, null);

    // 이메일 중복 검사 이후 동시 요청으로 DB 고유 제약 조건에 걸리는 것을 방지한다.
    try {
      User saved = userRepository.saveAndFlush(user);
      return userMapper.toDto(saved);
    } catch (DataIntegrityViolationException e) {
      throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email), e);
    }
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
            .findById(targetUserId)
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
      UUID targetUserId, UUID requesterUserId, UserRoleUpdateRequest userRoleUpdateRequest) {
    User user =
        userRepository
            .findByIdAndIsDeletedFalse(targetUserId)
            .orElseThrow(
                () ->
                    new UserException(
                        UserErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId)));

    if (user.getRole() == userRoleUpdateRequest.role()) {
      return;
    }

    user.changeRole(userRoleUpdateRequest.role());
    authSessionService.invalidateAllByUserId(targetUserId);
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
}
