package com.sb10.mopl.auth.security.oauth;

import com.sb10.mopl.auth.entity.SocialProvider;
import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class Oauth2UserProfileResolverComposite {

  private final Map<SocialProvider, Oauth2UserProfileResolver> resolvers;

  public Oauth2UserProfileResolverComposite(List<Oauth2UserProfileResolver> resolvers) {
    this.resolvers =
        resolvers.stream()
            .collect(
                Collectors.toMap(
                    Oauth2UserProfileResolver::provider,
                    Function.identity(),
                    (left, right) -> left,
                    () -> new EnumMap<>(SocialProvider.class)));
  }

  public Oauth2UserProfile resolve(SocialProvider provider, Map<String, Object> attributes) {
    Oauth2UserProfileResolver resolver = resolvers.get(provider);
    if (resolver == null) {
      throw new MoplException(
          AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", "지원하지 않는 소셜 로그인 제공자입니다."));
    }
    return resolver.resolve(attributes);
  }
}
