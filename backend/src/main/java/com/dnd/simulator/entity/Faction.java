package com.dnd.simulator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
public class Faction {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  @Setter
  private String name;

  @Setter
  private String alignment;
  private int power;
  private int wealth;
  private int manpower;
  private int influence;
  private int territory;
  @Setter
  private String imageUrl;

  @Setter
  private String description;

  @ManyToOne(optional = false)
  @Setter
  private World world;

  public Faction(String name, String alignment, int power, String description, World world) {
    this.name = name;
    this.alignment = alignment;
    this.power = power;
    this.description = description;
    this.world = world;
  }

  public void setPower(int power) {
    this.power = clamp(power);
  }

  public void setWealth(int wealth) {
    this.wealth = clamp(wealth);
  }

  public void setManpower(int manpower) {
    this.manpower = clamp(manpower);
  }

  public void setInfluence(int influence) {
    this.influence = clamp(influence);
  }

  public void setTerritory(int territory) {
    this.territory = Math.max(0, Math.min(100, territory));
  }

  @JsonIgnore
  public World getWorld() {
    return world;
  }

  private int clamp(int value) {
    return Math.max(0, Math.min(100, value));
  }
}
