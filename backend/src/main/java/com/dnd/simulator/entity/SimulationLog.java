package com.dnd.simulator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class SimulationLog {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private Instant createdAt = Instant.now();

  @Column(length = 3000)
  private String message;

  @ManyToOne(optional = false)
  private World world;

  public SimulationLog(String message, World world) {
    this.message = message;
    this.world = world;
  }

  @JsonIgnore
  public World getWorld() {
    return world;
  }
}
