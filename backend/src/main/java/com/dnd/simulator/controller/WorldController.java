package com.dnd.simulator.controller;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.service.FactionService;
import com.dnd.simulator.service.WorldEventService;
import com.dnd.simulator.service.WorldService;
import com.dnd.simulator.service.WorldSimulationService;
import com.dnd.simulator.service.WorldSimulationScheduler;
import com.dnd.simulator.service.WorldSnapshotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {
  private final WorldService worlds;
  private final FactionService factions;
  private final WorldEventService events;
  private final WorldSimulationService simulations;
  private final WorldSimulationScheduler simulationScheduler;
  private final WorldSnapshotService snapshots;
  private final ObjectMapper mapper;

  public WorldController(
      WorldService worlds,
      FactionService factions,
      WorldEventService events,
      WorldSimulationService simulations,
      WorldSimulationScheduler simulationScheduler,
      WorldSnapshotService snapshots,
      ObjectMapper mapper) {
    this.worlds = worlds;
    this.factions = factions;
    this.events = events;
    this.simulations = simulations;
    this.simulationScheduler = simulationScheduler;
    this.snapshots = snapshots;
    this.mapper = mapper;
  }

  private String userId(Authentication authentication) {
    return authentication.getName();
  }

  @GetMapping
  public ResponseEntity<List<?>> list(Authentication authentication) {
    return ResponseEntity.ok(worlds.list(userId(authentication)));
  }

  @PostMapping
  public ResponseEntity<Object> create(
      @Valid @RequestBody ApiDtos.WorldRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(worlds.create(request, userId(authentication)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> data(
      @PathVariable String id, Authentication authentication) {
    return ResponseEntity.ok(snapshots.data(id, userId(authentication)));
  }

  @PostMapping("/{id}/factions")
  public ResponseEntity<Object> faction(
      @PathVariable String id,
      @Valid @RequestBody ApiDtos.FactionRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(factions.createFaction(id, request, userId(authentication)));
  }

  @PutMapping("/{id}/factions/{factionId}")
  public ResponseEntity<Object> updateFaction(
      @PathVariable String id,
      @PathVariable String factionId,
      @Valid @RequestBody ApiDtos.FactionRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        factions.updateFaction(id, factionId, request, userId(authentication)));
  }

  @DeleteMapping("/{id}/factions/{factionId}")
  public ResponseEntity<Void> deleteFaction(
      @PathVariable String id,
      @PathVariable String factionId,
      Authentication authentication) {
    factions.deleteFaction(id, factionId, userId(authentication));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/events")
  public ResponseEntity<Object> event(
      @PathVariable String id,
      @Valid @RequestBody ApiDtos.EventRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(events.createEvent(id, request, userId(authentication)));
  }

  @PutMapping("/{id}/events/{eventId}")
  public ResponseEntity<Object> updateEvent(
      @PathVariable String id,
      @PathVariable String eventId,
      @Valid @RequestBody ApiDtos.EventRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        events.updateEvent(id, eventId, request, userId(authentication)));
  }

  @DeleteMapping("/{id}/events/{eventId}")
  public ResponseEntity<Void> deleteEvent(
      @PathVariable String id,
      @PathVariable String eventId,
      Authentication authentication) {
    events.deleteEvent(id, eventId, userId(authentication));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/simulate")
  public ResponseEntity<Object> simulate(
      @PathVariable String id,
      Authentication authentication) {
    return ResponseEntity.ok(simulations.simulate(id, userId(authentication)));
  }

  @PostMapping("/{id}/simulation/start")
  public ResponseEntity<ApiDtos.SimulationStatusResponse> startSimulation(
      @PathVariable String id,
      @Valid @RequestBody ApiDtos.SimulationControlRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        simulationScheduler.start(id, userId(authentication), request));
  }

  @PostMapping("/{id}/simulation/stop")
  public ResponseEntity<ApiDtos.SimulationStatusResponse> stopSimulation(
      @PathVariable String id,
      Authentication authentication) {
    return ResponseEntity.ok(simulationScheduler.stop(id, userId(authentication)));
  }

  @GetMapping("/{id}/simulation/status")
  public ResponseEntity<ApiDtos.SimulationStatusResponse> simulationStatus(
      @PathVariable String id,
      Authentication authentication) {
    return ResponseEntity.ok(simulationScheduler.status(id, userId(authentication)));
  }

  @PostMapping("/import")
  public ResponseEntity<Object> importWorld(
      @RequestBody JsonNode snapshot,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(snapshots.importSnapshot(snapshot, userId(authentication)));
  }

  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable String id,
      Authentication authentication)
      throws JsonProcessingException {
    Map<String, Object> snapshot = snapshots.data(id, userId(authentication));
    String json = mapper.writeValueAsString(snapshot);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=world-" + id + ".json")
        .body(json.getBytes());
  }
}
