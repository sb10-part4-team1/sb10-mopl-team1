package com.sb10.mopl.auth.security.jwt;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.security.handler.AuthErrorResponseWriter;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.JwtSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;
  private final JwtSessionService jwtSessionService;
  private final AuthenticatedUserFactory authenticatedUserFactory;
  private final AuthErrorResponseWriter responseWriter;
  private final List<RequestMatcher> skipRequestMatchers;
  private final List<RequestMatcher> continueOnFailureRequestMatchers;

  public JwtAuthenticationFilter(
      JwtProvider jwtProvider,
      JwtSessionService jwtSessionService,
      AuthenticatedUserFactory authenticatedUserFactory,
      AuthErrorResponseWriter responseWriter,
      RequestMatcher[] skipRequestMatchers,
      RequestMatcher[] continueOnFailureRequestMatchers) {
    this.jwtProvider = jwtProvider;
    this.jwtSessionService = jwtSessionService;
    this.authenticatedUserFactory = authenticatedUserFactory;
    this.responseWriter = responseWriter;
    this.skipRequestMatchers = List.of(skipRequestMatchers);
    this.continueOnFailureRequestMatchers = List.of(continueOnFailureRequestMatchers);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return skipRequestMatchers.stream().anyMatch(matcher -> matcher.matches(request));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(AUTHORIZATION_HEADER);

    if (authorization == null || authorization.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!authorization.startsWith(BEARER_PREFIX)) {
      if (shouldContinueOnAuthenticationFailure(request)) {
        filterChain.doFilter(request, response);
        return;
      }
      writeAuthenticationFailure(response, "Authorization header must use Bearer token.");
      return;
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (token.isBlank()) {
      if (shouldContinueOnAuthenticationFailure(request)) {
        filterChain.doFilter(request, response);
        return;
      }
      writeAuthenticationFailure(response, "Bearer token is empty.");
      return;
    }

    try {
      Claims claims = jwtProvider.parseClaims(token);
      AuthenticatedUser authenticatedUser = toAuthenticatedUser(claims);
      verifyAdditionalTokenPolicy(claims, authenticatedUser);

      SecurityContextHolder.getContext().setAuthentication(createAuthentication(authenticatedUser));

      filterChain.doFilter(request, response);
    } catch (JwtException | IllegalArgumentException exception) {
      SecurityContextHolder.clearContext();
      if (shouldContinueOnAuthenticationFailure(request)) {
        filterChain.doFilter(request, response);
        return;
      }
      writeAuthenticationFailure(response, "Invalid or expired access token.");
    }
  }

  private boolean shouldContinueOnAuthenticationFailure(HttpServletRequest request) {
    return continueOnFailureRequestMatchers.stream().anyMatch(matcher -> matcher.matches(request));
  }

  private AuthenticatedUser toAuthenticatedUser(Claims claims) {
    String subject = claims.getSubject();
    String id = requiredClaim(claims, "id");
    String email = requiredClaim(claims, "email");
    String role = requiredClaim(claims, "role");
    String tokenType = requiredClaim(claims, JwtProvider.TOKEN_TYPE_CLAIM);

    if (subject == null || subject.isBlank() || !subject.equals(id)) {
      throw new IllegalArgumentException("JWT subject does not match id claim.");
    }

    if (!JwtProvider.ACCESS_TOKEN_TYPE.equals(tokenType)) {
      throw new IllegalArgumentException("JWT token type is not ACCESS.");
    }

    return new AuthenticatedUser(UUID.fromString(id), email, UserRole.valueOf(role));
  }

  protected void verifyAdditionalTokenPolicy(Claims claims, AuthenticatedUser authenticatedUser) {
    UUID sessionId =
        UUID.fromString(
            authenticatedUserFactory.requiredClaim(claims, JwtProvider.SESSION_ID_CLAIM));
    if (!jwtSessionService.isActive(authenticatedUser.id(), sessionId)) {
      throw new IllegalArgumentException("JWT session is not active.");
    }
  }

  private String requiredClaim(Claims claims, String name) {
    Object value = claims.get(name);
    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      throw new IllegalArgumentException("Missing JWT claim: " + name);
    }
    return stringValue;
  }

  private Authentication createAuthentication(AuthenticatedUser authenticatedUser) {
    return UsernamePasswordAuthenticationToken.authenticated(
        authenticatedUser,
        null,
        List.of(new SimpleGrantedAuthority(authenticatedUser.authorityName())));
  }

  private void writeAuthenticationFailure(HttpServletResponse response, String message)
      throws IOException {
    SecurityContextHolder.clearContext();
    responseWriter.write(response, AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", message));
  }
}
