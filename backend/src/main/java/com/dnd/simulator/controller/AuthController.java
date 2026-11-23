package com.dnd.simulator.controller;

import com.dnd.simulator.dto.AuthDtos;
import com.dnd.simulator.service.AuthService;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final String REFRESH_COOKIE = "refresh_token";
  private final AuthService service;
  private final boolean cookieSecure;
  private final String cookieSameSite;
  private final long refreshExpirationMs;

  public AuthController(
      AuthService service,
      @Value("${app.cookie-secure:false}") boolean cookieSecure,
      @Value("${app.cookie-same-site:Strict}") String cookieSameSite,
      @Value("${app.refresh-expiration-ms:1209600000}") long refreshExpirationMs) {
    this.service = service;
    this.cookieSecure = cookieSecure;
    this.cookieSameSite = cookieSameSite;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthDtos.TokenPair> register(
      @Valid @RequestBody AuthDtos.Credentials credentials) {
    AuthService.AuthResult result = service.register(credentials);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(result.tokens());
  }

  @PostMapping("/login")
  public ResponseEntity<AuthDtos.TokenPair> login(
      @Valid @RequestBody AuthDtos.Credentials credentials) {
    AuthService.AuthResult result = service.login(credentials);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(result.tokens());
  }

  @GetMapping("/csrf")
  public AuthDtos.CsrfResponse csrf(CsrfToken csrfToken) {
    return new AuthDtos.CsrfResponse(
        csrfToken.getHeaderName(),
        csrfToken.getToken());
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthDtos.TokenPair> refresh(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("Refresh token is missing");
    }
    AuthService.AuthResult result = service.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(result.tokens());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
    service.logout(refreshToken);
    ResponseCookie expiredCookie =
        ResponseCookie.from(REFRESH_COOKIE, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/api/auth")
            .maxAge(Duration.ZERO)
            .build();
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .build();
  }

  private ResponseCookie refreshCookie(String refreshToken) {
    return ResponseCookie.from(REFRESH_COOKIE, refreshToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(cookieSameSite)
        .path("/api/auth")
        .maxAge(Duration.ofMillis(refreshExpirationMs))
        .build();
  }
}
