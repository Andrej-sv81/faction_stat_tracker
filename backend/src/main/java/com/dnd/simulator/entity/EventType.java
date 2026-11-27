package com.dnd.simulator.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "event_types")
public class EventType {
  @Id
  @Column(nullable = false, length = 64, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 120)
  private String domain;

  @Column(length = 1000)
  private String description;

  @Column(nullable = false)
  private boolean enabled = true;

  public EventType(String code, String name, String domain, String description) {
    this.code = code;
    this.name = name;
    this.domain = domain;
    this.description = description;
    this.enabled = true;
  }

  @JsonValue
  public String jsonCode() {
    return code;
  }
}
