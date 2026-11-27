package com.dnd.simulator.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long accessExpirationMs;

  public JwtService(
      @Value("${app.jwt-secret}") String secret,
      @Value("${app.jwt-access-expiration-ms:900000}") long accessExpirationMs) {
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessExpirationMs = accessExpirationMs;
  }

  public String createAccessToken(String userId) {
    java.util.Date now = new java.util.Date();
    return Jwts.builder()
        .subject(userId)
        .claim("type", "access")
        .claim("jti", java.util.UUID.randomUUID().toString())
        .issuedAt(now)
        .expiration(new java.util.Date(now.getTime() + accessExpirationMs))
        .signWith(key)
        .compact();
  }

  public String userId(String token) {
    var claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    if (!"access".equals(claims.get("type", String.class))) {
      throw new io.jsonwebtoken.JwtException("Token is not an access token");
    }
    return claims.getSubject();
  }

  public long accessExpirationMs() {
    return accessExpirationMs;
  }
}
