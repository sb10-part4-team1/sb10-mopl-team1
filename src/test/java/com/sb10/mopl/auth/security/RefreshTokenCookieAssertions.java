package com.sb10.mopl.auth.security;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.sb10.mopl.auth.security.jwt.JwtProperties;
import java.util.regex.Pattern;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

public final class RefreshTokenCookieAssertions {

  private RefreshTokenCookieAssertions() {}

  public static ResultActions expectRefreshTokenCookie(
      ResultActions actions, JwtProperties jwtProperties) throws Exception {
    JwtProperties.RefreshTokenCookie cookieProperties = jwtProperties.refreshTokenCookie();
    String cookieName = cookieProperties.name();

    return actions
        .andExpect(cookie().exists(cookieName))
        .andExpect(cookie().httpOnly(cookieName, cookieProperties.httpOnly()))
        .andExpect(cookie().secure(cookieName, cookieProperties.secure()))
        .andExpect(cookie().path(cookieName, cookieProperties.path()))
        .andExpect(
            header()
                .stringValues(
                    HttpHeaders.SET_COOKIE,
                    Matchers.hasItem(
                        Matchers.allOf(
                            Matchers.containsString(cookieName + "="),
                            Matchers.matchesPattern(
                                ".*\\bMax-Age="
                                    + jwtProperties.refreshTokenExpiration().toSeconds()
                                    + "\\b.*"),
                            Matchers.matchesPattern(
                                ".*\\bPath=" + Pattern.quote(cookieProperties.path()) + "\\b.*"),
                            Matchers.containsString("HttpOnly"),
                            Matchers.containsString("SameSite=" + cookieProperties.sameSite())))));
  }
}
