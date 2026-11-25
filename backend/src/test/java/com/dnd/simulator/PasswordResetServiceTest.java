package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dnd.simulator.entity.AppUser;
import com.dnd.simulator.entity.PasswordResetToken;
import com.dnd.simulator.repository.AppUserRepository;
import com.dnd.simulator.repository.PasswordResetTokenRepository;
import com.dnd.simulator.service.PasswordResetEmailRequested;
import com.dnd.simulator.service.PasswordResetService;
import com.dnd.simulator.service.RefreshTokenService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {
  @Test
  void sendsResetLinkAndPersistsOnlyTokenHash() {
    AppUserRepository users = mock(AppUserRepository.class);
    PasswordResetTokenRepository resetTokens =
        mock(PasswordResetTokenRepository.class);
    RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    ApplicationEventPublisher eventPublisher =
        mock(ApplicationEventPublisher.class);
    AppUser user = mock(AppUser.class);
    when(user.getId()).thenReturn("user-1");
    when(user.getEmail()).thenReturn("keeper@example.com");
    when(users.findByEmail("keeper@example.com")).thenReturn(Optional.of(user));
    when(resetTokens.findByUser_IdAndConsumedAtIsNull("user-1"))
        .thenReturn(List.of());
    when(resetTokens.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PasswordResetService service =
        new PasswordResetService(
            users,
            resetTokens,
            refreshTokens,
            encoder,
            eventPublisher,
            900_000,
            "https://example.com/",
            "noreply@example.com");

    service.requestReset(" KEEPER@Example.com ");

    var tokenCaptor =
        org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
    var eventCaptor =
        org.mockito.ArgumentCaptor.forClass(PasswordResetEmailRequested.class);
    verify(resetTokens).save(tokenCaptor.capture());
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    String resetUrl = eventCaptor.getValue().resetUrl();
    Matcher matcher =
        Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(resetUrl);
    assertTrue(matcher.find());
    assertNotEquals(matcher.group(1), tokenCaptor.getValue().getTokenHash());
    assertTrue(tokenCaptor.getValue().getExpiresAt().isAfter(Instant.now()));
    assertTrue(resetUrl.startsWith("https://example.com/reset-password#token="));
  }

  @Test
  void unknownEmailDoesNotSendEmailOrCreateToken() {
    AppUserRepository users = mock(AppUserRepository.class);
    PasswordResetTokenRepository resetTokens =
        mock(PasswordResetTokenRepository.class);
    ApplicationEventPublisher eventPublisher =
        mock(ApplicationEventPublisher.class);
    when(users.findByEmail("unknown@example.com"))
        .thenReturn(Optional.empty());

    PasswordResetService service =
        new PasswordResetService(
            users,
            resetTokens,
            mock(RefreshTokenService.class),
            mock(PasswordEncoder.class),
            eventPublisher,
            900_000,
            "http://localhost:5173",
            "noreply@localhost");

    assertDoesNotThrow(() -> service.requestReset("unknown@example.com"));

    verifyNoInteractions(resetTokens, eventPublisher);
  }

  @Test
  void successfulResetChangesPasswordConsumesTokenAndRevokesSessions() {
    AppUserRepository users = mock(AppUserRepository.class);
    PasswordResetTokenRepository resetTokens =
        mock(PasswordResetTokenRepository.class);
    RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    AppUser user = mock(AppUser.class);
    when(user.getId()).thenReturn("user-2");
    PasswordResetToken token =
        new PasswordResetToken(
            "hashed-token",
            user,
            Instant.now(),
            Instant.now().plusSeconds(900));
    when(resetTokens.findByTokenHashForUpdate(anyString()))
        .thenReturn(Optional.of(token));
    when(encoder.encode("new-password-123")).thenReturn("encoded-password");

    PasswordResetService service =
        new PasswordResetService(
            users,
            resetTokens,
            refreshTokens,
            encoder,
            mock(ApplicationEventPublisher.class),
            900_000,
            "http://localhost:5173",
            "noreply@localhost");

    service.resetPassword("raw-token", "new-password-123");

    verify(user).setPasswordHash("encoded-password");
    verify(users).save(user);
    verify(resetTokens).save(token);
    verify(refreshTokens).revokeUserSessions("user-2");
    assertTrue(token.getConsumedAt() != null);
  }

  @Test
  void rejectsExpiredResetTokenWithoutChangingPassword() {
    PasswordResetTokenRepository resetTokens =
        mock(PasswordResetTokenRepository.class);
    AppUser user = mock(AppUser.class);
    PasswordResetToken token =
        new PasswordResetToken(
            "hashed-token",
            user,
            Instant.now().minusSeconds(1),
            Instant.now().minusSeconds(1));
    when(resetTokens.findByTokenHashForUpdate(anyString()))
        .thenReturn(Optional.of(token));
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    AppUserRepository users = mock(AppUserRepository.class);
    RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    PasswordResetService service =
        new PasswordResetService(
            users,
            resetTokens,
            refreshTokens,
            encoder,
            mock(ApplicationEventPublisher.class),
            900_000,
            "http://localhost:5173",
            "noreply@localhost");

    assertThrows(
        IllegalArgumentException.class,
        () -> service.resetPassword("raw-token", "new-password-123"));

    verifyNoInteractions(encoder, users, refreshTokens);
  }
}
