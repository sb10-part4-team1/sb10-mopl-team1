package com.sb10.mopl.auth.oauth;

import com.sb10.mopl.auth.entity.SocialProvider;
import java.util.Map;

public interface Oauth2UserProfileResolver {

  SocialProvider provider();

  Oauth2UserProfile resolve(Map<String, Object> attributes);
}
