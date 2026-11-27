package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.World;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dnd.simulator.repository.SimulationLogRepository;

@Service
public class WorldSnapshotService {
  private final WorldService worlds;
  private final FactionService factions;
  private final WorldEventService events;
  private final SimulationLogRepository logs;

  public WorldSnapshotService(
      WorldService worlds,
      FactionService factions,
      WorldEventService events,
      SimulationLogRepository logs) {
    this.worlds = worlds;
    this.factions = factions;
    this.events = events;
    this.logs = logs;
  }

  @Transactional
  public World importSnapshot(JsonNode root, String ownerId) {
    JsonNode source = root.has("world") ? root.get("world") : root;
    if (!source.hasNonNull("name") || source.path("name").asText().isBlank()) {
      throw new IllegalArgumentException("Import must contain a world name");
    }
    World world =
        worlds.create(
            new ApiDtos.WorldRequest(
                source.path("name").asText(), source.path("description").asText("")),
            ownerId);
    if (root.has("factions") && root.get("factions").isArray()) {
      for (JsonNode faction : root.get("factions")) {
        factions.createFaction(
            world.getId(),
            new ApiDtos.FactionRequest(
                faction.path("name").asText("Faction"),
                faction.path("alignment").asText("Neutral"),
                faction.path("power").asInt(50),
                faction.path("wealth").asInt(50),
                faction.path("manpower").asInt(50),
                faction.path("influence").asInt(50),
                faction.path("territory").asInt(1),
                faction.path("imageUrl").asText(null),
                faction.path("description").asText(""),
                null),
            ownerId);
      }
    }
    if (root.has("events") && root.get("events").isArray()) {
      for (JsonNode event : root.get("events")) {
        String typeCode = parseEventTypeCode(event.path("type").asText());
        String importedCategory = event.path("category").asText();
        String targetFactionId = event.path("targetFactionId").asText(null);
        String secondTargetFactionId = event.path("secondTargetFactionId").asText(null);
        if ("global".equalsIgnoreCase(importedCategory)) {
          targetFactionId = null;
          secondTargetFactionId = null;
        }
        events.createEvent(
            world.getId(),
            new ApiDtos.EventRequest(
                event.path("title").asText("Imported event"),
                typeCode,
                targetFactionId,
                secondTargetFactionId,
                event.path("powerDelta").asInt(),
                event.path("wealthDelta").asInt(),
                event.path("manpowerDelta").asInt(),
                event.path("influenceDelta").asInt(),
                event.path("territoryDelta").asInt(),
                event.path("details").asText(""),
                null),
            ownerId);
      }
    }
    return world;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> data(String worldId, String ownerId) {
    World world = worlds.get(worldId, ownerId);
    return Map.of(
        "world",
        world,
        "factions",
        factions.findByWorldId(worldId),
        "events",
        events.findByWorldId(worldId),
        "logs",
        logs.findTop100ByWorldIdOrderByCreatedAtDesc(worldId));
  }

  private String parseEventTypeCode(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Imported events must use a supported event type");
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }
}
