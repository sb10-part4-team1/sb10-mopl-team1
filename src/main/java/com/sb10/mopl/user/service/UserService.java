package com.sb10.mopl.user.service;

import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
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
}
