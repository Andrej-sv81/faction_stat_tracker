package com.dnd.simulator.dto;

import jakarta.validation.constraints.*;

public final class ApiDtos {
  public record WorldRequest(
      @NotBlank @Size(max = 120) String name, @Size(max = 1000) String description) {}

  public record FactionRequest(
      @NotBlank @Size(max = 120) String name,
      @Size(max = 60) String alignment,
      @Min(0) @Max(100) int power,
      @Min(0) @Max(100) int wealth,
      @Min(0) @Max(100) int manpower,
      @Min(0) @Max(100) int influence,
      @Min(0) @Max(100) int territory,
      @Size(max = 500) String imageUrl,
      @Size(max = 1000) String description) {}

  public record EventRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 64) String type,
      String targetFactionId,
      String secondTargetFactionId,
      @Min(-100) @Max(100) int powerDelta,
      @Min(-100) @Max(100) int wealthDelta,
      @Min(-100) @Max(100) int manpowerDelta,
      @Min(-100) @Max(100) int influenceDelta,
      @Min(-20) @Max(20) int territoryDelta,
      @Size(max = 2000) String details) {}

  public record EventTypeResponse(String code, String name, String domain, String description) {}
}
