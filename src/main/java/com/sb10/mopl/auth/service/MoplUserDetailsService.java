package com.sb10.mopl.auth.service;

import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MoplUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) {
    User user =
        userRepository
            .findByEmailAndIsDeletedFalse(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new MoplUserDetails(user);
  }
}
