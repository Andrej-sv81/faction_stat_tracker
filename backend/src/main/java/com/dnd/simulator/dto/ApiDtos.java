package com.dnd.simulator.dto;
import jakarta.validation.constraints.*;
public final class ApiDtos {
 public record WorldRequest(@NotBlank @Size(max=120) String name,@Size(max=1000) String description){}
 public record FactionRequest(@NotBlank @Size(max=120) String name,@Size(max=60) String alignment,@Min(0) @Max(100) int power,@Size(max=1000) String description){}
 public record EventRequest(@NotBlank @Size(max=120) String title,@NotBlank String type,@Size(max=2000) String details){}
}
