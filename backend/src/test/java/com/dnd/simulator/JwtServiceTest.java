package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dnd.simulator.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private final JwtService jwt =
      new JwtService(
          "development-only-secret-that-is-at-least-32-characters",
          60_000);

  @Test
  void usesUuidAsSubject() {
    String userId = UUID.randomUUID().toString();

    String token = jwt.createAccessToken(userId);

    assertEquals(userId, jwt.userId(token));
  }

  @Test
  void rejectsInvalidSignature() {
    JwtService otherJwt =
        new JwtService(
            "different-development-secret-that-is-at-least-32-characters",
            60_000);

    String token = jwt.createAccessToken(UUID.randomUUID().toString());

    assertThrows(JwtException.class, () -> otherJwt.userId(token));
  }
}
