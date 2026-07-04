package com.sb10.mopl.auth.service;

import com.sb10.mopl.auth.email.TemporaryPasswordEmailSender;
import com.sb10.mopl.auth.entity.TemporaryPassword;
import com.sb10.mopl.auth.util.TemporaryPasswordGenerator;
import com.sb10.mopl.auth.repository.TemporaryPasswordRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemporaryPasswordService {

  private static final Duration TEMPORARY_PASSWORD_EXPIRATION = Duration.ofMinutes(3);

  private final TemporaryPasswordRepository temporaryPasswordRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TemporaryPasswordGenerator temporaryPasswordGenerator;
  private final TemporaryPasswordEmailSender temporaryPasswordEmailSender;
  private final Clock clock;

  @Transactional
  public void resetPassword(String email) {
    User user =
        userRepository
            .findByEmailAndIsDeletedFalse(email)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("email", email)));

    temporaryPasswordRepository.deleteByUserId(user.getId());

    String temporaryPassword = temporaryPasswordGenerator.generate();
    String passwordHash = passwordEncoder.encode(temporaryPassword);
    Instant expiresAt = clock.instant().plus(TEMPORARY_PASSWORD_EXPIRATION);

    temporaryPasswordRepository.save(TemporaryPassword.create(user, passwordHash, expiresAt));
    temporaryPasswordEmailSender.send(user.getEmail(), temporaryPassword);
  }
}
