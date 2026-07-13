package com.sb10.mopl.auth.service;

import com.sb10.mopl.auth.entity.SocialAccount;
import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.oauth.Oauth2UserProfile;
import com.sb10.mopl.auth.repository.SocialAccountRepository;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

  private final SocialAccountRepository socialAccountRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public User findOrLinkUser(Oauth2UserProfile profile) {
    return socialAccountRepository
        .findByProviderAndProviderUserIdWithUser(profile.provider(), profile.providerUserId())
        .map(SocialAccount::getUser)
        .map(this::validateLoginAllowed)
        .orElseGet(() -> linkUser(profile));
  }

  private User linkUser(Oauth2UserProfile profile) {
    User user =
        userRepository
            .findByEmailAndIsDeletedFalse(profile.email())
            .orElseGet(() -> createUser(profile));
    validateLoginAllowed(user);
    socialAccountRepository.save(
        SocialAccount.create(user, profile.provider(), profile.providerUserId()));
    return user;
  }

  private User createUser(Oauth2UserProfile profile) {
    return userRepository.save(
        User.createUser(
            profile.name(),
            profile.email(),
            passwordEncoder.encode(UUID.randomUUID().toString()),
            profile.profileImageUrl()));
  }

  private User validateLoginAllowed(User user) {
    if (user.isLocked()) {
      throw new MoplException(
          AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", "잠긴 계정은 로그인할 수 없습니다."));
    }
    return user;
  }
}
