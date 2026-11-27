package com.dnd.simulator.service;

import com.dnd.simulator.entity.AppUser;
import com.dnd.simulator.entity.RefreshToken;
import com.dnd.simulator.exception.AuthenticationException;
import com.dnd.simulator.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
  public record Rotation(String rawToken, AppUser user) {}

  private final RefreshTokenRepository refreshTokens;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long refreshExpirationMs;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokens,
      @Value("${app.refresh-expiration-ms:1209600000}") long refreshExpirationMs) {
    this.refreshTokens = refreshTokens;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  @Transactional
  public String createSession(AppUser user) {
    return createSession(user, UUID.randomUUID().toString(), Instant.now());
  }

  @Transactional
  public Rotation rotate(String rawToken) {
    RefreshToken current = findActive(rawToken);
    Instant now = Instant.now();
    String replacementRawToken =
        createSession(current.getUser(), current.getFamilyId(), now);

    current.revoke(now);
    RefreshToken replacement =
        refreshTokens
            .findByTokenHash(hash(replacementRawToken))
            .orElseThrow(
                () -> new IllegalStateException("Refresh token replacement was not saved"));
    current.markReplacedBy(replacement.getId());
    refreshTokens.save(current);
    return new Rotation(replacementRawToken, current.getUser());
  }

  @Transactional
  public void revoke(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    RefreshToken token =
        refreshTokens
            .findByTokenHash(hash(rawToken))
            .orElse(null);
    if (token == null) {
      return;
    }
    revokeFamily(token.getFamilyId(), Instant.now());
  }

  @Transactional
  public void revokeUserSessions(String userId) {
    for (RefreshToken token :
        refreshTokens.findByUser_IdAndRevokedAtIsNull(userId)) {
      revokeFamily(token.getFamilyId(), Instant.now());
    }
  }

  private RefreshToken findActive(String rawToken) {
    RefreshToken token =
        refreshTokens
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

    Instant now = Instant.now();
    if (token.getRevokedAt() != null) {
      revokeFamily(token.getFamilyId(), now);
      throw new AuthenticationException("Refresh token reuse detected");
    }
    if (!token.getExpiresAt().isAfter(now)) {
      throw new AuthenticationException("Refresh token expired");
    }
    return token;
  }

  private String createSession(AppUser user, String familyId, Instant now) {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    String rawToken =
        Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    RefreshToken token =
        new RefreshToken(
            hash(rawToken),
            familyId,
            user,
            now,
            now.plusMillis(refreshExpirationMs));
    refreshTokens.save(token);
    return rawToken;
  }

  private void revokeFamily(String familyId, Instant revokedAt) {
    for (RefreshToken token : refreshTokens.findByFamilyId(familyId)) {
      if (token.getRevokedAt() == null) {
        token.revoke(revokedAt);
        refreshTokens.save(token);
      }
    }
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
