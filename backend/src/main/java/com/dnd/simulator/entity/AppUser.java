package com.dnd.simulator.entity;

import jakarta.persistence.*; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Table(name = "app_users")
public class AppUser {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
  @Column(nullable=false, unique=true) private String email;
  @Column(nullable=false) private String passwordHash;
  public AppUser() {}
  public AppUser(String email, String passwordHash) { this.email=email; this.passwordHash=passwordHash; }
  public String getId(){return id;} public String getEmail(){return email;} @JsonIgnore public String getPasswordHash(){return passwordHash;}
  public void setEmail(String email){this.email=email;} public void setPasswordHash(String hash){this.passwordHash=hash;}
}
