package com.dnd.simulator.service;

import com.dnd.simulator.dto.AuthDtos;
import com.dnd.simulator.entity.AppUser;
import com.dnd.simulator.repository.AppUserRepository;
import com.dnd.simulator.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final AppUserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthService(AppUserRepository u, PasswordEncoder e, JwtService j) {
    users = u;
    encoder = e;
    jwt = j;
  }

  public AuthDtos.Token register(AuthDtos.Credentials c) {
    if (users.findByEmail(c.email()).isPresent())
      throw new IllegalArgumentException("Email already registered");
    users.save(new AppUser(c.email().toLowerCase(), encoder.encode(c.password())));
    return new AuthDtos.Token(jwt.create(c.email().toLowerCase()), c.email().toLowerCase());
  }

  public AuthDtos.Token login(AuthDtos.Credentials c) {
    var u =
        users
            .findByEmail(c.email().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    if (!encoder.matches(c.password(), u.getPasswordHash()))
      throw new IllegalArgumentException("Invalid credentials");
    return new AuthDtos.Token(jwt.create(u.getEmail()), u.getEmail());
  }
}
