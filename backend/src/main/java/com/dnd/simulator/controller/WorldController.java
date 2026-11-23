package com.dnd.simulator.controller;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.service.WorldService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {
  private final WorldService service;
  private final ObjectMapper mapper;

  public WorldController(WorldService worldService, ObjectMapper objectMapper) {
    service = worldService;
    mapper = objectMapper;
  }

  private String userId(Authentication authentication) {
    return authentication.getName();
  }

  @GetMapping
  public ResponseEntity<List<?>> list(Authentication authentication) {
    return ResponseEntity.ok(service.list(userId(authentication)));
  }

  @PostMapping
  public ResponseEntity<Object> create(
      @Valid @RequestBody ApiDtos.WorldRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(request, userId(authentication)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> data(
      @PathVariable String id, Authentication authentication) {
    var world = service.get(id, userId(authentication));
    var response = new HashMap<>(service.data(id, userId(authentication)));
    response.put("world", world);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{id}/factions")
  public ResponseEntity<Object> faction(
      @PathVariable String id,
      @Valid @RequestBody ApiDtos.FactionRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.faction(id, request, userId(authentication)));
  }

  @PutMapping("/{id}/factions/{factionId}")
  public ResponseEntity<Object> updateFaction(
      @PathVariable String id,
      @PathVariable String factionId,
      @Valid @RequestBody ApiDtos.FactionRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        service.updateFaction(
            id, factionId, request, userId(authentication)));
  }

  @DeleteMapping("/{id}/factions/{factionId}")
  public ResponseEntity<Void> deleteFaction(
      @PathVariable String id,
      @PathVariable String factionId,
      Authentication authentication) {
    service.deleteFaction(id, factionId, userId(authentication));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/events")
  public ResponseEntity<Object> event(
      @PathVariable String id,
      @Valid @RequestBody ApiDtos.EventRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.event(id, request, userId(authentication)));
  }

  @PutMapping("/{id}/events/{eventId}")
  public ResponseEntity<Object> updateEvent(
      @PathVariable String id,
      @PathVariable String eventId,
      @Valid @RequestBody ApiDtos.EventRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        service.updateEvent(
            id, eventId, request, userId(authentication)));
  }

  @DeleteMapping("/{id}/events/{eventId}")
  public ResponseEntity<Void> deleteEvent(
      @PathVariable String id,
      @PathVariable String eventId,
      Authentication authentication) {
    service.deleteEvent(id, eventId, userId(authentication));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/simulate")
  public ResponseEntity<Object> simulate(
      @PathVariable String id,
      Authentication authentication) {
    return ResponseEntity.ok(
        service.simulate(id, userId(authentication)));
  }

  @PostMapping("/import")
  public ResponseEntity<Object> importWorld(
      @RequestBody JsonNode snapshot,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.importSnapshot(snapshot, userId(authentication)));
  }

  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable String id,
      Authentication authentication)
      throws JsonProcessingException {
    var snapshot =
        new HashMap<>(service.data(id, userId(authentication)));
    snapshot.put("world", service.get(id, userId(authentication)));
    String json = mapper.writeValueAsString(snapshot);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=world-" + id + ".json")
        .body(json.getBytes());
  }
}
