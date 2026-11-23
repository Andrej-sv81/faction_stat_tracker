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
@Table(name = "refresh_tokens")
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  private String tokenHash;

  @Column(nullable = false)
  private String familyId;

  @ManyToOne(optional = false)
  private AppUser user;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant revokedAt;
  private String replacedById;

  public RefreshToken(
      String tokenHash,
      String familyId,
      AppUser user,
      Instant createdAt,
      Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.familyId = familyId;
    this.user = user;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public void revoke(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public void markReplacedBy(String replacementId) {
    this.replacedById = replacementId;
  }

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }
}
