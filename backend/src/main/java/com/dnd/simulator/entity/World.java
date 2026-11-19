package com.dnd.simulator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
public class World {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Setter
  private String name;

  @Setter
  private String description;
  private Instant createdAt = Instant.now();

  @ManyToOne(optional = false)
  private AppUser owner;

  public World(String name, String description, AppUser owner) {
    this.name = name;
    this.description = description;
    this.owner = owner;
  }

}
