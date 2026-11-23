package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dnd.simulator.exception.TooManyRequestsException;
import com.dnd.simulator.service.AuthenticationThrottleService;
import org.junit.jupiter.api.Test;

class AuthenticationThrottleServiceTest {
  private final AuthenticationThrottleService throttle =
      new AuthenticationThrottleService(2, 900, 2, 3600);

  @Test
  void limitsLoginAttemptsByIpAndEmail() {
    assertDoesNotThrow(
        () -> throttle.checkLoginAttempt("127.0.0.1", "keeper@example.com"));
    assertDoesNotThrow(
        () -> throttle.checkLoginAttempt("127.0.0.1", "keeper@example.com"));

    assertThrows(
        TooManyRequestsException.class,
        () -> throttle.checkLoginAttempt("127.0.0.1", "keeper@example.com"));
  }

  @Test
  void successfulLoginResetsIpAndEmailAttempts() {
    assertDoesNotThrow(
        () -> throttle.checkLoginAttempt("127.0.0.2", "reset@example.com"));
    assertDoesNotThrow(
        () -> throttle.checkLoginAttempt("127.0.0.2", "reset@example.com"));

    throttle.resetLoginAttempt("127.0.0.2", "reset@example.com");

    assertDoesNotThrow(
        () -> throttle.checkLoginAttempt("127.0.0.2", "reset@example.com"));
  }

  @Test
  void limitsRegistrationAttemptsByIp() {
    assertDoesNotThrow(
        () -> throttle.checkRegistrationAttempt("192.0.2.10"));
    assertDoesNotThrow(
        () -> throttle.checkRegistrationAttempt("192.0.2.10"));

    assertThrows(
        TooManyRequestsException.class,
        () -> throttle.checkRegistrationAttempt("192.0.2.10"));
  }

  @Test
  void successfulRegistrationResetsIpAttempts() {
    assertDoesNotThrow(
        () -> throttle.checkRegistrationAttempt("192.0.2.11"));
    assertDoesNotThrow(
        () -> throttle.checkRegistrationAttempt("192.0.2.11"));

    throttle.resetRegistrationAttempt("192.0.2.11");

    assertDoesNotThrow(
        () -> throttle.checkRegistrationAttempt("192.0.2.11"));
  }
}
