package com.dnd.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  private String tokenHash;

  @ManyToOne(optional = false)
  private AppUser user;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant consumedAt;

  public PasswordResetToken(
      String tokenHash,
      AppUser user,
      Instant createdAt,
      Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.user = user;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public void consume(Instant consumedAt) {
    this.consumedAt = consumedAt;
  }

  public boolean isActive(Instant now) {
    return consumedAt == null && expiresAt.isAfter(now);
  }
}
