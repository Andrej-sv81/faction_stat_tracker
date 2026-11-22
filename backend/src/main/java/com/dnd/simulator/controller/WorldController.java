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

  public WorldController(WorldService s, ObjectMapper m) {
    service = s;
    mapper = m;
  }

  private String userId(Authentication a) {
    return a.getName();
  }

  @GetMapping
  public List<?> list(Authentication a) {
    return service.list(userId(a));
  }

  @PostMapping
  public Object create(@Valid @RequestBody ApiDtos.WorldRequest r, Authentication a) {
    return service.create(r, userId(a));
  }

  @GetMapping("/{id}")
  public Object data(@PathVariable String id, Authentication a) {
    var w = service.get(id, userId(a));
    var d = new HashMap<>(service.data(id, userId(a)));
    d.put("world", w);
    return d;
  }

  @PostMapping("/{id}/factions")
  public Object faction(
      @PathVariable String id, @Valid @RequestBody ApiDtos.FactionRequest r, Authentication a) {
    return service.faction(id, r, userId(a));
  }

  @PutMapping("/{id}/factions/{factionId}")
  public Object updateFaction(
      @PathVariable String id,
      @PathVariable String factionId,
      @Valid @RequestBody ApiDtos.FactionRequest r,
      Authentication a) {
    return service.updateFaction(id, factionId, r, userId(a));
  }

  @DeleteMapping("/{id}/factions/{factionId}")
  public void deleteFaction(
      @PathVariable String id, @PathVariable String factionId, Authentication a) {
    service.deleteFaction(id, factionId, userId(a));
  }

  @PostMapping("/{id}/events")
  public Object event(
      @PathVariable String id, @Valid @RequestBody ApiDtos.EventRequest r, Authentication a) {
    return service.event(id, r, userId(a));
  }

  @PutMapping("/{id}/events/{eventId}")
  public Object updateEvent(
      @PathVariable String id,
      @PathVariable String eventId,
      @Valid @RequestBody ApiDtos.EventRequest r,
      Authentication a) {
    return service.updateEvent(id, eventId, r, userId(a));
  }

  @DeleteMapping("/{id}/events/{eventId}")
  public void deleteEvent(@PathVariable String id, @PathVariable String eventId, Authentication a) {
    service.deleteEvent(id, eventId, userId(a));
  }

  @PostMapping("/{id}/simulate")
  public Object simulate(@PathVariable String id, Authentication a) {
    return service.simulate(id, userId(a));
  }

  @PostMapping("/import")
  public Object importWorld(@RequestBody JsonNode snapshot, Authentication a) {
    return service.importSnapshot(snapshot, userId(a));
  }

  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> export(@PathVariable String id, Authentication a)
      throws JsonProcessingException {
    var snapshot = new HashMap<>(service.data(id, userId(a)));
    snapshot.put("world", service.get(id, userId(a)));
    String json = mapper.writeValueAsString(snapshot);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=world-" + id + ".json")
        .body(json.getBytes());
  }
}
