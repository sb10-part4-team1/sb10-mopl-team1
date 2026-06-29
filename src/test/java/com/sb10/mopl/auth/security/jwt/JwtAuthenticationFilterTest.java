package com.sb10.mopl.auth.security.jwt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.handler.AuthErrorResponseWriter;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {

  private static final Instant NOW = Instant.parse("2026-06-28T00:00:00Z");
  private static final String SECRET = "mopl-test-only-jwt-secret-key-at-least-32-bytes";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("유효한 전달 토큰 요청이면 보안 컨텍스트에 인증 객체를 저장한다")
  void doFilter_success_whenBearerTokenIsValid() throws Exception {
    UUID userId = UUID.randomUUID();
    String token =
        jwtProviderAt(NOW)
            .createAccessToken(userDetails(userId, "jwt-user@example.com", UserRole.USER));
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW));
    MockHttpServletRequest request = request("/api/protected");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    AuthenticatedUser principal =
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
    assertAll(
        () -> assertTrue(chainInvoked.get()),
        () -> assertEquals(200, response.getStatus()),
        () -> assertEquals(userId, principal.id()),
        () -> assertEquals("jwt-user@example.com", principal.email()),
        () -> assertEquals(UserRole.USER, principal.role()),
        () -> assertTrue(authentication.isAuthenticated()),
        () ->
            assertTrue(
                authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority()))));
  }

  @Test
  @DisplayName("토큰이 없는 요청이면 인증 객체 없이 필터 체인을 통과한다")
  void doFilter_success_whenAuthorizationHeaderIsMissing() throws Exception {
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW));
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request("/api/protected"), response, chain(chainInvoked));

    assertAll(
        () -> assertTrue(chainInvoked.get()),
        () -> assertEquals(200, response.getStatus()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
  }

  @Test
  @DisplayName("공개 경로 요청이면 인증 헤더가 유효하지 않아도 필터를 건너뛴다")
  void doFilter_success_whenPublicEndpointHasInvalidToken() throws Exception {
    RequestMatcher publicMatcher = request -> "/api/public".equals(request.getRequestURI());
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(
            jwtProviderAt(NOW),
            new AuthErrorResponseWriter(objectMapper),
            new RequestMatcher[] {publicMatcher});
    MockHttpServletRequest request = request("/api/public");
    request.addHeader(HttpHeaders.AUTHORIZATION, "NotBearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    assertAll(
        () -> assertTrue(chainInvoked.get()),
        () -> assertEquals(200, response.getStatus()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
  }

  @Test
  @DisplayName("잘못된 전달 토큰 요청이면 401 인증 실패를 반환한다")
  void doFilter_fail_whenTokenIsMalformed() throws Exception {
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW));
    MockHttpServletRequest request = request("/api/protected");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer malformed-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertAll(
        () -> assertEquals(401, response.getStatus()),
        () -> assertEquals("AUTH01", body.get("code").asText()),
        () ->
            assertEquals(
                "Invalid or expired access token.", body.get("details").get("message").asText()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
        () -> assertTrue(!chainInvoked.get()));
  }

  @Test
  @DisplayName("만료된 전달 토큰 요청이면 401 인증 실패를 반환한다")
  void doFilter_fail_whenTokenIsExpired() throws Exception {
    String token =
        jwtProviderAt(NOW)
            .createAccessToken(
                userDetails(UUID.randomUUID(), "expired-user@example.com", UserRole.USER));
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW.plus(Duration.ofHours(2))));
    MockHttpServletRequest request = request("/api/protected");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertAll(
        () -> assertEquals(401, response.getStatus()),
        () -> assertEquals("AUTH01", body.get("code").asText()),
        () ->
            assertEquals(
                "Invalid or expired access token.", body.get("details").get("message").asText()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
        () -> assertTrue(!chainInvoked.get()));
  }

  @Test
  @DisplayName("Bearer 형식이 아닌 인증 헤더면 401 인증 실패를 반환한다")
  void doFilter_fail_whenAuthorizationHeaderDoesNotUseBearerScheme() throws Exception {
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW));
    MockHttpServletRequest request = request("/api/protected");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credential");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertAll(
        () -> assertEquals(401, response.getStatus()),
        () -> assertEquals("AUTH01", body.get("code").asText()),
        () ->
            assertEquals(
                "Authorization header must use Bearer token.",
                body.get("details").get("message").asText()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
        () -> assertTrue(!chainInvoked.get()));
  }

  @Test
  @DisplayName("Bearer 토큰 값이 비어 있으면 401 인증 실패를 반환한다")
  void doFilter_fail_whenBearerTokenIsBlank() throws Exception {
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW));
    MockHttpServletRequest request = request("/api/protected");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertAll(
        () -> assertEquals(401, response.getStatus()),
        () -> assertEquals("AUTH01", body.get("code").asText()),
        () -> assertEquals("Bearer token is empty.", body.get("details").get("message").asText()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
        () -> assertTrue(!chainInvoked.get()));
  }

  @Test
  @DisplayName("JWT subject와 id 클레임이 다르면 401 인증 실패를 반환한다")
  void doFilter_fail_whenSubjectDoesNotMatchIdClaim() throws Exception {
    UUID id = UUID.randomUUID();
    String token =
        tokenBuilder()
            .subject(UUID.randomUUID().toString())
            .claim("id", id.toString())
            .claim("email", "jwt-user@example.com")
            .claim("role", UserRole.USER.name())
            .claim(JwtProvider.TOKEN_TYPE_CLAIM, JwtProvider.ACCESS_TOKEN_TYPE)
            .compact();

    assertInvalidToken(token);
  }

  @Test
  @DisplayName("JWT 토큰 타입이 ACCESS가 아니면 401 인증 실패를 반환한다")
  void doFilter_fail_whenTokenTypeIsNotAccess() throws Exception {
    UUID id = UUID.randomUUID();
    String token =
        tokenBuilder()
            .subject(id.toString())
            .claim("id", id.toString())
            .claim("email", "jwt-user@example.com")
            .claim("role", UserRole.USER.name())
            .claim(JwtProvider.TOKEN_TYPE_CLAIM, "REFRESH")
            .compact();

    assertInvalidToken(token);
  }

  @Test
  @DisplayName("JWT 필수 클레임이 비어 있으면 401 인증 실패를 반환한다")
  void doFilter_fail_whenRequiredClaimIsBlank() throws Exception {
    UUID id = UUID.randomUUID();
    String token =
        tokenBuilder()
            .subject(id.toString())
            .claim("id", id.toString())
            .claim("email", "")
            .claim("role", UserRole.USER.name())
            .claim(JwtProvider.TOKEN_TYPE_CLAIM, JwtProvider.ACCESS_TOKEN_TYPE)
            .compact();

    assertInvalidToken(token);
  }

  private JwtAuthenticationFilter jwtFilter(JwtProvider jwtProvider) {
    return new JwtAuthenticationFilter(
        jwtProvider, new AuthErrorResponseWriter(objectMapper), new RequestMatcher[0]);
  }

  private JwtProvider jwtProviderAt(Instant instant) {
    JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(1));
    return new JwtProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
  }

  private MockHttpServletRequest request(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRequestURI(uri);
    return request;
  }

  private FilterChain chain(AtomicBoolean invoked) {
    return (request, response) -> invoked.set(true);
  }

  private JwtBuilder tokenBuilder() {
    return Jwts.builder()
        .issuedAt(Date.from(NOW))
        .expiration(Date.from(NOW.plus(Duration.ofHours(1))))
        .signWith(secretKey(), Jwts.SIG.HS256);
  }

  private SecretKey secretKey() {
    return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }

  private void assertInvalidToken(String token) throws Exception {
    JwtAuthenticationFilter filter = jwtFilter(jwtProviderAt(NOW));
    MockHttpServletRequest request = request("/api/protected");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainInvoked));

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertAll(
        () -> assertEquals(401, response.getStatus()),
        () -> assertEquals("AUTH01", body.get("code").asText()),
        () ->
            assertEquals(
                "Invalid or expired access token.", body.get("details").get("message").asText()),
        () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
        () -> assertTrue(!chainInvoked.get()));
  }

  private MoplUserDetails userDetails(UUID id, String email, UserRole role) {
    User user =
        role == UserRole.ADMIN
            ? User.createAdmin("jwt-user", email, "encoded-password", null)
            : User.createUser("jwt-user", email, "encoded-password", null);
    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "createdAt", NOW);
    return new MoplUserDetails(user);
  }
}
