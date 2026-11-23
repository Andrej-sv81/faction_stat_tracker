package com.dnd.simulator.service;

import com.dnd.simulator.exception.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationThrottleService {
  private final Map<String, AttemptWindow> attemptWindows = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int loginAttemptsLimit;
  private final Duration loginWindow;
  private final int registrationAttemptsLimit;
  private final Duration registrationWindow;

  public AuthenticationThrottleService(
      @Value("${app.auth.login-attempt-limit:5}") int loginAttemptsLimit,
      @Value("${app.auth.login-window-seconds:900}") long loginWindowSeconds,
      @Value("${app.auth.registration-attempt-limit:3}") int registrationAttemptsLimit,
      @Value("${app.auth.registration-window-seconds:3600}") long registrationWindowSeconds) {
    this.clock = Clock.systemUTC();
    this.loginAttemptsLimit = loginAttemptsLimit;
    this.loginWindow = Duration.ofSeconds(loginWindowSeconds);
    this.registrationAttemptsLimit = registrationAttemptsLimit;
    this.registrationWindow = Duration.ofSeconds(registrationWindowSeconds);
  }

  public void checkLoginAttempt(String clientAddress, String email) {
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
    check("login-ip:" + clientAddress, loginAttemptsLimit, loginWindow);
    check("login-email:" + normalizedEmail, loginAttemptsLimit, loginWindow);
  }

  public void resetLoginAttempt(String clientAddress, String email) {
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
    attemptWindows.remove("login-ip:" + clientAddress);
    attemptWindows.remove("login-email:" + normalizedEmail);
  }

  public void checkRegistrationAttempt(String clientAddress) {
    check(
        "registration-ip:" + clientAddress,
        registrationAttemptsLimit,
        registrationWindow);
  }

  public void resetRegistrationAttempt(String clientAddress) {
    attemptWindows.remove("registration-ip:" + clientAddress);
  }

  @Scheduled(fixedDelayString = "${app.auth.cleanup-delay-ms:300000}")
  public void cleanupExpiredWindows() {
    Instant now = clock.instant();
    Iterator<Map.Entry<String, AttemptWindow>> iterator =
        attemptWindows.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, AttemptWindow> entry = iterator.next();
      if (!entry.getValue().expiresAt().isAfter(now)) {
        iterator.remove();
      }
    }
  }

  private synchronized void check(String key, int limit, Duration window) {
    Instant now = clock.instant();
    AttemptWindow current = attemptWindows.get(key);
    if (current == null || !current.expiresAt().isAfter(now)) {
      attemptWindows.put(
          key,
          new AttemptWindow(1, now.plus(window)));
      return;
    }
    if (current.count() >= limit) {
      long retryAfterSeconds =
          Math.max(1, Duration.between(now, current.expiresAt()).getSeconds());
      throw new TooManyRequestsException(
          "Too many authentication attempts. Please try again later.",
          retryAfterSeconds);
    }
    attemptWindows.put(
        key,
        new AttemptWindow(current.count() + 1, current.expiresAt()));
  }

  private record AttemptWindow(int count, Instant expiresAt) {}
}
