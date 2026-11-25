package com.dnd.simulator.service;

public record PasswordResetEmailRequested(
    String userId,
    String email,
    String resetUrl,
    long expirationMinutes,
    String fromAddress) {}
