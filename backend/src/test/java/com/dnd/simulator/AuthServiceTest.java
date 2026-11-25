package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dnd.simulator.dto.AuthDtos;
import com.dnd.simulator.exception.DuplicateResourceException;
import com.dnd.simulator.repository.AppUserRepository;
import com.dnd.simulator.security.JwtService;
import com.dnd.simulator.service.AuthService;
import com.dnd.simulator.service.RefreshTokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
  @Test
  void rejectsExistingNormalizedEmail() {
    AppUserRepository users = mock(AppUserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    JwtService jwt = mock(JwtService.class);
    RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    AuthService authService =
        new AuthService(users, encoder, jwt, refreshTokens);

    when(users.findByEmail("keeper@example.com"))
        .thenReturn(Optional.of(mock(com.dnd.simulator.entity.AppUser.class)));

    assertThrows(
        DuplicateResourceException.class,
        () ->
            authService.register(
                new AuthDtos.Credentials(
                    "  KEEPER@Example.com  ",
                    "password123")));

    verify(users).findByEmail("keeper@example.com");
  }

  @Test
  void translatesDatabaseDuplicateRaceToConflict() {
    AppUserRepository users = mock(AppUserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    JwtService jwt = mock(JwtService.class);
    RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    AuthService authService =
        new AuthService(users, encoder, jwt, refreshTokens);

    when(users.findByEmail("race@example.com"))
        .thenReturn(Optional.empty());
    when(encoder.encode("password123"))
        .thenReturn("encoded-password");
    when(users.save(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new DataIntegrityViolationException("duplicate email"));

    assertThrows(
        DuplicateResourceException.class,
        () ->
            authService.register(
                new AuthDtos.Credentials(
                    " RACE@EXAMPLE.COM ",
                    "password123")));
  }
}
