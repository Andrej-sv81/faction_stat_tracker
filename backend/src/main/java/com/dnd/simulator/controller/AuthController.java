package com.dnd.simulator.controller;

import com.dnd.simulator.dto.AuthDtos;
import com.dnd.simulator.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService s) {
    service = s;
  }

  @PostMapping("/register")
  public AuthDtos.Token register(@Valid @RequestBody AuthDtos.Credentials c) {
    return service.register(c);
  }

  @PostMapping("/login")
  public AuthDtos.Token login(@Valid @RequestBody AuthDtos.Credentials c) {
    return service.login(c);
  }
}
