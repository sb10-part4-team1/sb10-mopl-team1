package com.sb10.mopl.auth.security.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HttpCookieOauth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  public static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
  private static final Duration COOKIE_MAX_AGE = Duration.ofMinutes(3);
  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
  private static final String SAME_SITE = "Lax";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String COOKIE_VALUE_DELIMITER = ".";

  private final JwtProperties jwtProperties;

  private static ObjectMapper createObjectMapper() {
    ClassLoader classLoader = HttpCookieOauth2AuthorizationRequestRepository.class.getClassLoader();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
    objectMapper.registerModule(new OAuth2ClientJackson2Module());
    return objectMapper;
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return findCookie(request).map(this::deserialize).orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      removeAuthorizationRequestCookie(response);
      return;
    }
    addAuthorizationRequestCookie(response, serialize(authorizationRequest), COOKIE_MAX_AGE);
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
    removeAuthorizationRequestCookie(response);
    return authorizationRequest;
  }

  private void removeAuthorizationRequestCookie(HttpServletResponse response) {
    addAuthorizationRequestCookie(response, "", Duration.ZERO);
  }

  private void addAuthorizationRequestCookie(
      HttpServletResponse response, String value, Duration maxAge) {
    JwtProperties.RefreshTokenCookie refreshTokenCookie = jwtProperties.refreshTokenCookie();
    ResponseCookie cookie =
        ResponseCookie.from(AUTHORIZATION_REQUEST_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(refreshTokenCookie.secure())
            .sameSite(SAME_SITE)
            .path("/")
            .maxAge(maxAge)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private Optional<Cookie> findCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> AUTHORIZATION_REQUEST_COOKIE_NAME.equals(cookie.getName()))
        .findFirst();
  }

  private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(authorizationRequest);
      String payload = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
      return payload + COOKIE_VALUE_DELIMITER + sign(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("OAuth2AuthorizationRequest 직렬화에 실패했습니다.", e);
    }
  }

  private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
    try {
      String value = cookie.getValue();
      int delimiterIndex = value.indexOf(COOKIE_VALUE_DELIMITER);
      if (delimiterIndex < 0) {
        return null;
      }
      String payload = value.substring(0, delimiterIndex);
      String signature = value.substring(delimiterIndex + 1);
      byte[] expectedSignature = sign(payload).getBytes(StandardCharsets.UTF_8);
      byte[] actualSignature = signature.getBytes(StandardCharsets.UTF_8);
      if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
        return null;
      }
      byte[] json = Base64.getUrlDecoder().decode(payload);
      return OBJECT_MAPPER.readValue(json, OAuth2AuthorizationRequest.class);
    } catch (IOException | IllegalArgumentException e) {
      return null;
    }
  }

  private String sign(String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(
              jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().encodeToString(signatureBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("OAuth2AuthorizationRequest 쿠키 서명 생성에 실패했습니다.", e);
    }
  }
}
