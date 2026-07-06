package com.sb10.mopl.user.admin;

import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

  private final AdminAccountProperties adminAccountProperties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthSessionService authSessionService;
  private final TransactionTemplate transactionTemplate;

  @Override
  public void run(ApplicationArguments args) {
    if (!adminAccountProperties.enabled()) {
      log.info("Admin account initialization is disabled.");
      return;
    }

    try {
      initializeAdminAccount();
    } catch (RuntimeException e) {
      log.error("Failed to initialize admin account.", e);
      throw e;
    }
  }

  private void initializeAdminAccount() {
    adminAccountProperties.validateForInitialization();

    String email = adminAccountProperties.email();
    try {
      transactionTemplate.executeWithoutResult(
          status -> initializeAdminAccountInTransaction(email));
    } catch (AdminAccountCreateException e) {
      recoverFromCreateFailure(email, e);
    }
  }

  private void initializeAdminAccountInTransaction(String email) {
    userRepository
        .findByEmail(email)
        .ifPresentOrElse(this::calibrateAdminAccount, this::createAdminAccount);
  }

  private void calibrateExistingAdminAccount(String email) {
    User admin =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Admin account was not found."));
    calibrateAdminAccount(admin);
  }

  private void recoverFromCreateFailure(String email, AdminAccountCreateException createFailure) {
    try {
      transactionTemplate.executeWithoutResult(status -> calibrateExistingAdminAccount(email));
      log.info("Recovered admin account initialization after create conflict. email={}", email);
    } catch (RuntimeException recoveryFailure) {
      createFailure.addSuppressed(recoveryFailure);
      throw createFailure;
    }
  }

  private void calibrateAdminAccount(User admin) {
    if (admin.isDeleted()) {
      throw new IllegalStateException("Configured admin account is deleted.");
    }

    if (admin.getRole() != UserRole.ADMIN) {
      throw new IllegalStateException(
          "Configured admin account email belongs to a non-admin user.");
    }

    boolean changed = false;

    if (admin.isLocked()) {
      admin.changeLocked(false);
      changed = true;
    }

    if (adminAccountProperties.overwritePassword()) {
      admin.changePassword(passwordEncoder.encode(adminAccountProperties.password()));
      changed = true;
    }

    if (changed) {
      userRepository.flush();
      authSessionService.invalidateAllByUserId(admin.getId());
      log.info("Calibrated existing admin account. email={}", admin.getEmail());
      return;
    }

    log.info("Admin account already satisfies initialization policy. email={}", admin.getEmail());
  }

  private void createAdminAccount() {
    String encodedPassword = passwordEncoder.encode(adminAccountProperties.password());
    User admin =
        User.createAdmin(
            adminAccountProperties.name(), adminAccountProperties.email(), encodedPassword, null);
    try {
      userRepository.saveAndFlush(admin);
    } catch (DataIntegrityViolationException e) {
      throw new AdminAccountCreateException(e);
    }
    log.info("Created initial admin account. email={}", adminAccountProperties.email());
  }

  private static class AdminAccountCreateException extends RuntimeException {

    private AdminAccountCreateException(Throwable cause) {
      super(cause);
    }
  }
}
