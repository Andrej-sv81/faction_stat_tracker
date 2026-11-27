package com.dnd.simulator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
  @ManyToOne(optional = false)
  @JoinColumn(name = "event_type_code", nullable = false)
  private EventType type;

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

  public WorldEvent(
      String title,
      EventType type,
      String targetFactionId,
      String details,
      World world) {
    this.title = title;
    this.type = type;
    this.targetFactionId = targetFactionId;
    this.details = details;
    this.world = world;
  }

  @JsonProperty("category")
  public String getCategory() {
    return type == null ? null : type.getDomain();
  }

  @JsonIgnore
  public World getWorld() {
    return world;
  }
}
