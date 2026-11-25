package com.dnd.simulator.service;

import com.dnd.simulator.dto.AuthDtos;
import com.dnd.simulator.entity.AppUser;
import com.dnd.simulator.exception.AuthenticationException;
import com.dnd.simulator.exception.DuplicateResourceException;
import com.dnd.simulator.repository.AppUserRepository;
import com.dnd.simulator.security.JwtService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final AppUserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final RefreshTokenService refreshTokens;

  public AuthService(
      AppUserRepository users,
      PasswordEncoder encoder,
      JwtService jwt,
      RefreshTokenService refreshTokens) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
    this.refreshTokens = refreshTokens;
  }

  public AuthResult register(AuthDtos.Credentials credentials) {
    String normalizedEmail =
        credentials.email().trim().toLowerCase(Locale.ROOT);
    if (users.findByEmail(normalizedEmail).isPresent())
      throw new DuplicateResourceException("Email already registered");
    AppUser user;
    try {
      user =
          users.save(
              new AppUser(
                  normalizedEmail,
                  encoder.encode(credentials.password())));
    } catch (DataIntegrityViolationException exception) {
      throw new DuplicateResourceException("Email already registered");
    }
    return createResult(user);
  }

  public AuthResult login(AuthDtos.Credentials credentials) {
    String normalizedEmail =
        credentials.email().trim().toLowerCase(Locale.ROOT);
    AppUser user =
        users
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
    if (!encoder.matches(credentials.password(), user.getPasswordHash()))
      throw new AuthenticationException("Invalid credentials");
    return createResult(user);
  }

  public AuthResult refresh(String rawRefreshToken) {
    RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
    return createResult(rotation.user(), rotation.rawToken());
  }

  public void logout(String rawRefreshToken) {
    refreshTokens.revoke(rawRefreshToken);
  }

  private AuthResult createResult(AppUser user) {
    return createResult(user, refreshTokens.createSession(user));
  }

  private AuthResult createResult(AppUser user, String refreshToken) {
    String accessToken = jwt.createAccessToken(user.getId());
    AuthDtos.TokenPair tokens =
        new AuthDtos.TokenPair(
            accessToken,
            "Bearer",
            jwt.accessExpirationMs() / 1000,
            user.getEmail());
    return new AuthResult(tokens, refreshToken);
  }

  public record AuthResult(AuthDtos.TokenPair tokens, String refreshToken) {}
}
