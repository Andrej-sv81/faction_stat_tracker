package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.EventType;
import com.dnd.simulator.entity.Faction;
import com.dnd.simulator.entity.World;
import com.dnd.simulator.entity.WorldEvent;
import com.dnd.simulator.entity.SimulationLog;
import com.dnd.simulator.repository.SimulationLogRepository;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldSimulationService {
  private final WorldService worlds;
  private final FactionService factions;
  private final WorldEventService events;
  private final SimulationLogRepository logs;
  private final Random random = new Random();

  public WorldSimulationService(
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
  public Map<String, Object> simulate(String worldId, String ownerId) {
    return simulate(worldId, ownerId, 3);
  }

  @Transactional
  public Map<String, Object> simulate(String worldId, String ownerId, int severity) {
    if (severity < 1 || severity > 5) {
      throw new IllegalArgumentException("Simulation severity must be between 1 and 5");
    }
    World world = worlds.get(worldId, ownerId);
    List<Faction> available = factions.findByWorldId(worldId);
    if (available.isEmpty()) {
      String message = "The realm waits for its first faction.";
      logs.save(new SimulationLog(message, world));
      return Map.of("message", message, "factions", available);
    }

    List<EventType> availableTypes = events.enabledTypes();
    if (availableTypes.isEmpty()) {
      throw new IllegalStateException("No enabled event types are configured");
    }

    boolean hasSecondTarget = available.size() > 1 && random.nextBoolean();
    EventType type = availableTypes.get(random.nextInt(availableTypes.size()));
    Faction first = available.get(random.nextInt(available.size()));
    Faction second =
        available.size() > 1
            ? available.stream()
                .filter(faction -> !faction.getId().equals(first.getId()))
                .findAny()
                .orElse(first)
            : first;
    RandomEventImpact.StatDeltas impacts =
        RandomEventImpact.forEvent(
            type.getCode(),
            type.getDomain(),
            severity * 2 + random.nextInt(severity * 2 + 1),
            random);
    String description =
        type.getDescription() == null || type.getDescription().isBlank()
            ? type.getName()
            : type.getDescription().strip();
    if (description.endsWith(".")) {
      description = description.substring(0, description.length() - 1);
    }
    ApiDtos.EventRequest request =
        new ApiDtos.EventRequest(
            "Random " + type.getName() + " event",
            type.getCode(),
            first.getId(),
            hasSecondTarget ? second.getId() : null,
            impacts.powerDelta(),
            impacts.wealthDelta(),
            impacts.manpowerDelta(),
            impacts.influenceDelta(),
            impacts.territoryDelta(),
            description + ". Effects: " + impacts.summary() + ".",
            severity,
            null);
    WorldEvent created = events.createForWorld(world, request);
    return Map.of(
        "message", created.getTitle(), "factions", factions.findByWorldId(worldId));
  }
}
