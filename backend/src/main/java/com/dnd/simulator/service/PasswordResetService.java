package com.dnd.simulator.service;

import com.dnd.simulator.entity.AppUser;
import com.dnd.simulator.entity.PasswordResetToken;
import com.dnd.simulator.repository.AppUserRepository;
import com.dnd.simulator.repository.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
  private final AppUserRepository users;
  private final PasswordResetTokenRepository resetTokens;
  private final RefreshTokenService refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long tokenExpirationMs;
  private final String frontendUrl;
  private final String fromAddress;

  public PasswordResetService(
      AppUserRepository users,
      PasswordResetTokenRepository resetTokens,
      RefreshTokenService refreshTokens,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher eventPublisher,
      @Value("${app.password-reset.expiration-ms:900000}") long tokenExpirationMs,
      @Value("${app.password-reset.frontend-url:http://localhost:5173}") String frontendUrl,
      @Value("${app.password-reset.from:noreply@localhost}") String fromAddress) {
    this.users = users;
    this.resetTokens = resetTokens;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.eventPublisher = eventPublisher;
    this.tokenExpirationMs = tokenExpirationMs;
    this.frontendUrl = frontendUrl;
    this.fromAddress = fromAddress;
  }

  @Transactional
  public void requestReset(String email) {
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    users.findByEmail(normalizedEmail).ifPresent(this::createAndSendResetToken);
  }

  @Transactional
  public void resetPassword(String rawToken, String newPassword) {
    PasswordResetToken token =
        resetTokens
            .findByTokenHashForUpdate(hash(rawToken))
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
    Instant now = Instant.now();
    if (!token.isActive(now)) {
      throw new IllegalArgumentException("Invalid or expired reset token");
    }

    AppUser user = token.getUser();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    users.save(user);
    token.consume(now);
    resetTokens.save(token);
    refreshTokens.revokeUserSessions(user.getId());
  }

  private void createAndSendResetToken(AppUser user) {
    Instant now = Instant.now();
    resetTokens
        .findByUser_IdAndConsumedAtIsNull(user.getId())
        .forEach(token -> token.consume(now));

    String rawToken = generateToken();
    resetTokens.save(
        new PasswordResetToken(
            hash(rawToken),
            user,
            now,
            now.plusMillis(tokenExpirationMs)));

    eventPublisher.publishEvent(
        new PasswordResetEmailRequested(
            user.getId(),
            user.getEmail(),
            resetUrl(rawToken),
            Math.max(1, Duration.ofMillis(tokenExpirationMs).toMinutes()),
            fromAddress));
  }

  private String generateToken() {
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  private String resetUrl(String rawToken) {
    String baseUrl =
        frontendUrl.endsWith("/")
            ? frontendUrl.substring(0, frontendUrl.length() - 1)
            : frontendUrl;
    return baseUrl + "/reset-password#token=" + rawToken;
  }

  private String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
