package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.EventType;
import com.dnd.simulator.entity.Faction;
import com.dnd.simulator.entity.World;
import com.dnd.simulator.entity.WorldEvent;
import com.dnd.simulator.entity.SimulationLog;
import com.dnd.simulator.repository.EventTypeRepository;
import com.dnd.simulator.repository.FactionRepository;
import com.dnd.simulator.repository.SimulationLogRepository;
import com.dnd.simulator.repository.WorldEventRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldEventService {
  private final WorldService worlds;
  private final FactionRepository factions;
  private final WorldEventRepository events;
  private final EventTypeRepository eventTypes;
  private final SimulationLogRepository logs;

  public WorldEventService(
      WorldService worlds,
      FactionRepository factions,
      WorldEventRepository events,
      EventTypeRepository eventTypes,
      SimulationLogRepository logs) {
    this.worlds = worlds;
    this.factions = factions;
    this.events = events;
    this.eventTypes = eventTypes;
    this.logs = logs;
  }

  @Transactional
  public WorldEvent createEvent(
      String worldId, ApiDtos.EventRequest request, String ownerId) {
    World world = worlds.get(worldId, ownerId);
    return createForWorld(world, request);
  }

  @Transactional
  public WorldEvent createForWorld(World world, ApiDtos.EventRequest request) {
    EventType type = findEventType(request.type());
    return applyEvent(
        new WorldEvent(
            request.title(),
            type,
            request.targetFactionId(),
            request.details(),
            world),
        request,
        world.getId());
  }

  private WorldEvent applyEvent(WorldEvent event, ApiDtos.EventRequest request, String worldId) {
    validateEventTargets(request);
    List<Faction> targets = new ArrayList<>();
    boolean hasPrimaryTarget =
        request.targetFactionId() != null && !request.targetFactionId().isBlank();
    boolean hasSecondTarget =
        request.secondTargetFactionId() != null && !request.secondTargetFactionId().isBlank();
    if (!hasPrimaryTarget) {
      targets.addAll(factions.findByWorldId(worldId));
    } else {
      targets.add(findFaction(request.targetFactionId(), worldId, "Target faction not found"));
      if (hasSecondTarget) {
        targets.add(
            findFaction(
                request.secondTargetFactionId(),
                worldId,
                "Second target faction not found"));
      }
    }

    event.setPowerDelta(request.powerDelta());
    event.setWealthDelta(request.wealthDelta());
    event.setManpowerDelta(request.manpowerDelta());
    event.setInfluenceDelta(request.influenceDelta());
    event.setTerritoryDelta(request.territoryDelta());
    event.setSecondTargetFactionId(request.secondTargetFactionId());
    targets.forEach(
        target -> {
          target.setPower(target.getPower() + request.powerDelta());
          target.setWealth(target.getWealth() + request.wealthDelta());
          target.setManpower(target.getManpower() + request.manpowerDelta());
          target.setInfluence(target.getInfluence() + request.influenceDelta());
          target.setTerritory(target.getTerritory() + request.territoryDelta());
          factions.save(target);
        });

    World world = targets.isEmpty() ? event.getWorld() : targets.get(0).getWorld();
    String changes =
        String.format(
            "Wealth %+d, Manpower %+d, Influence %+d, Territory %+d%%",
            request.wealthDelta(),
            request.manpowerDelta(),
            request.influenceDelta(),
            request.territoryDelta());
    logs.save(new SimulationLog(event.getTitle() + " — " + changes, world));
    return events.save(event);
  }

  private void validateEventTargets(ApiDtos.EventRequest request) {
    boolean hasPrimaryTarget =
        request.targetFactionId() != null && !request.targetFactionId().isBlank();
    boolean hasSecondTarget =
        request.secondTargetFactionId() != null && !request.secondTargetFactionId().isBlank();
    if (!hasPrimaryTarget && hasSecondTarget) {
      throw new IllegalArgumentException("A second target requires a primary target faction");
    }
    if (hasPrimaryTarget
        && hasSecondTarget
        && request.targetFactionId().equals(request.secondTargetFactionId())) {
      throw new IllegalArgumentException("Event targets must be different factions");
    }
  }

  private Faction findFaction(String factionId, String worldId, String errorMessage) {
    return factions
        .findByIdAndWorldId(factionId, worldId)
        .orElseThrow(() -> new NoSuchElementException(errorMessage));
  }

  private EventType findEventType(String code) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("Event type is required");
    }
    String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
    return eventTypes
        .findByCodeAndEnabledTrue(normalizedCode)
        .orElseThrow(
            () -> new IllegalArgumentException("Unsupported or disabled event type: " + code));
  }

  @Transactional(readOnly = true)
  public List<ApiDtos.EventTypeResponse> eventTypes() {
    return eventTypes.findByEnabledTrueOrderByDomainAscNameAsc().stream()
        .map(
            type ->
                new ApiDtos.EventTypeResponse(
                    type.getCode(), type.getName(), type.getDomain(), type.getDescription()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<EventType> enabledTypes() {
    return eventTypes.findByEnabledTrueOrderByDomainAscNameAsc();
  }

  @Transactional(readOnly = true)
  public List<WorldEvent> findByWorldId(String worldId) {
    return events.findByWorldIdOrderByOccurredAtDesc(worldId);
  }

  private List<Faction> priorTargets(WorldEvent event, String worldId) {
    if ((event.getTargetFactionId() == null || event.getTargetFactionId().isBlank())
        && (event.getSecondTargetFactionId() == null
            || event.getSecondTargetFactionId().isBlank())) {
      return factions.findByWorldId(worldId);
    }
    List<Faction> targets = new ArrayList<>();
    if (event.getTargetFactionId() != null) {
      factions
          .findByIdAndWorldId(event.getTargetFactionId(), worldId)
          .ifPresent(targets::add);
    }
    if (event.getSecondTargetFactionId() != null) {
      factions
          .findByIdAndWorldId(event.getSecondTargetFactionId(), worldId)
          .ifPresent(targets::add);
    }
    return targets;
  }

  @Transactional
  public WorldEvent updateEvent(
      String worldId, String eventId, ApiDtos.EventRequest request, String ownerId) {
    worlds.get(worldId, ownerId);
    validateEventTargets(request);
    EventType type = findEventType(request.type());
    WorldEvent old =
        events
            .findByIdAndWorldId(eventId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    if (request.version() == null) {
      throw new IllegalArgumentException("A version is required to update an event");
    }
    if (!request.version().equals(old.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(WorldEvent.class, eventId);
    }
    priorTargets(old, worldId)
        .forEach(
            target -> {
              target.setPower(target.getPower() - old.getPowerDelta());
              target.setWealth(target.getWealth() - old.getWealthDelta());
              target.setManpower(target.getManpower() - old.getManpowerDelta());
              target.setInfluence(target.getInfluence() - old.getInfluenceDelta());
              target.setTerritory(target.getTerritory() - old.getTerritoryDelta());
              factions.save(target);
            });
    old.setTitle(request.title());
    old.setType(type);
    old.setTargetFactionId(request.targetFactionId());
    old.setDetails(request.details());
    return applyEvent(old, request, worldId);
  }

  @Transactional
  public void deleteEvent(String worldId, String eventId, String ownerId) {
    worlds.get(worldId, ownerId);
    events.delete(
        events
            .findByIdAndWorldId(eventId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Event not found")));
  }
}
