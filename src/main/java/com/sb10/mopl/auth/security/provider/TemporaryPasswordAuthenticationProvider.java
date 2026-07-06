package com.sb10.mopl.auth.security.provider;

import com.sb10.mopl.auth.entity.TemporaryPassword;
import com.sb10.mopl.auth.repository.TemporaryPasswordRepository;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordAuthenticationProvider implements AuthenticationProvider {

  private final TemporaryPasswordRepository temporaryPasswordRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Override
  @Transactional(readOnly = true)
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getName();
    String rawPassword = String.valueOf(authentication.getCredentials());

    TemporaryPassword temporaryPassword =
        temporaryPasswordRepository
            .findByUserEmailWithUser(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid temporary password"));

    User user = temporaryPassword.getUser();
    if (user.isLocked()) {
      throw new LockedException("User account is locked");
    }

    Instant now = clock.instant();
    if (temporaryPassword.isExpired(now)
        || !passwordEncoder.matches(rawPassword, temporaryPassword.getPasswordHash())) {
      throw new BadCredentialsException("Invalid temporary password");
    }

    MoplUserDetails userDetails = new MoplUserDetails(user);
    UsernamePasswordAuthenticationToken authenticated =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, authentication.getCredentials(), userDetails.getAuthorities());
    authenticated.setDetails(authentication.getDetails());
    return authenticated;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
