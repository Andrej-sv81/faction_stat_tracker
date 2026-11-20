package com.dnd.simulator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  @Setter
  private String email;

  @Column(nullable = false)
  @Setter
  private String passwordHash;

  public AppUser(String email, String passwordHash) {
    this.email = email;
    this.passwordHash = passwordHash;
  }

  @JsonIgnore
  public String getPasswordHash() {
    return passwordHash;
  }
}
