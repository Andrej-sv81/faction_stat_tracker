package com.dnd.simulator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "world_events")
public class WorldEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  @Setter
  private String title;

  @Setter
  private String type;
  @Setter
  private String category;
  @Setter
  private String targetFactionId;
  @Setter
  private String secondTargetFactionId;
  @Setter
  private int powerDelta;
  @Setter
  private int wealthDelta;
  @Setter
  private int manpowerDelta;
  @Setter
  private int influenceDelta;
  @Setter
  private int territoryDelta;

  @Column(length = 2000)
  @Setter
  private String details;

  private Instant occurredAt = Instant.now();

  @ManyToOne(optional = false)
  @Setter
  private World world;

  public WorldEvent(String title, String type, String category, String targetFactionId, String details, World world) {
    this.title = title;
    this.type = type;
    this.category = category;
    this.targetFactionId = targetFactionId;
    this.details = details;
    this.world = world;
  }

  @JsonIgnore
  public World getWorld() {
    return world;
  }
}
