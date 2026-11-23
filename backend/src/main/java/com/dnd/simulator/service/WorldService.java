package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.*;
import com.dnd.simulator.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldService {
  private final WorldRepository worlds;
  private final AppUserRepository users;
  private final FactionRepository factions;
  private final WorldEventRepository events;
  private final SimulationLogRepository logs;
  private final Random random = new Random();

  public WorldService(
      WorldRepository worlds,
      AppUserRepository users,
      FactionRepository factions,
      WorldEventRepository events,
      SimulationLogRepository logs) {
    this.worlds = worlds;
    this.users = users;
    this.factions = factions;
    this.events = events;
    this.logs = logs;
  }

  public List<World> list(String ownerId) {
    return worlds.findByOwnerIdOrderByCreatedAtDesc(ownerId);
  }

  public World get(String id, String ownerId) {
    return worlds
        .findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new NoSuchElementException("World not found"));
  }

  public World create(ApiDtos.WorldRequest request, String ownerId) {
    return worlds.save(
        new World(
            request.name(),
            request.description(),
            users.findById(ownerId).orElseThrow()));
  }

  public Faction faction(String id, ApiDtos.FactionRequest request, String ownerId) {
    World world = get(id, ownerId);
    return factions.save(
        fill(
            new Faction(
                request.name(), request.alignment(), request.power(), request.description(), world),
            request));
  }

  private Faction fill(Faction faction, ApiDtos.FactionRequest request) {
    faction.setName(request.name());
    faction.setAlignment(request.alignment());
    faction.setPower(request.power());
    faction.setWealth(request.wealth());
    faction.setManpower(request.manpower());
    faction.setInfluence(request.influence());
    faction.setTerritory(request.territory());
    faction.setImageUrl(request.imageUrl());
    faction.setDescription(request.description());
    return faction;
  }

  public Faction updateFaction(
      String worldId, String factionId, ApiDtos.FactionRequest request, String ownerId) {
    get(worldId, ownerId);
    Faction faction =
        factions
            .findByIdAndWorldId(factionId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Faction not found"));
    return factions.save(fill(faction, request));
  }

  public void deleteFaction(String worldId, String factionId, String ownerId) {
    get(worldId, ownerId);
    factions.delete(
        factions
            .findByIdAndWorldId(factionId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Faction not found")));
  }

  public WorldEvent event(String id, ApiDtos.EventRequest request, String ownerId) {
    World world = get(id, ownerId);
    return applyEvent(
        new WorldEvent(
            request.title(),
            request.type(),
            request.category(),
            request.targetFactionId(),
            request.details(),
            world),
        request,
        id);
  }

  private WorldEvent applyEvent(WorldEvent event, ApiDtos.EventRequest request, String worldId) {
    String category = request.category().toLowerCase(Locale.ROOT);
    List<Faction> targets = new ArrayList<>();
    if ("global".equals(category)) {
      targets.addAll(factions.findByWorldId(worldId));
    } else {
      if (request.targetFactionId() != null && !request.targetFactionId().isBlank()) {
        targets.add(
            factions
                .findByIdAndWorldId(request.targetFactionId(), worldId)
                .orElseThrow(() -> new NoSuchElementException("Target faction not found")));
      }
      if ("war".equals(category)
          && request.secondTargetFactionId() != null
          && !request.secondTargetFactionId().isBlank()) {
        targets.add(
            factions
                .findByIdAndWorldId(request.secondTargetFactionId(), worldId)
                .orElseThrow(() -> new NoSuchElementException("Second target faction not found")));
      }
    }
    if ("war".equals(category) && targets.size() != 2) {
      throw new IllegalArgumentException("War events require two different target factions");
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
    World world =
        targets.isEmpty()
            ? get(worldId, event.getWorld().getOwner().getId())
            : targets.get(0).getWorld();
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

  public WorldEvent updateEvent(
      String worldId, String eventId, ApiDtos.EventRequest request, String ownerId) {
    get(worldId, ownerId);
    WorldEvent old =
        events
            .findByIdAndWorldId(eventId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    List<String> oldTargets = new ArrayList<>();
    if (old.getTargetFactionId() != null) oldTargets.add(old.getTargetFactionId());
    if (old.getSecondTargetFactionId() != null) oldTargets.add(old.getSecondTargetFactionId());
    if ("global".equalsIgnoreCase(old.getCategory())) {
      oldTargets = factions.findByWorldId(worldId).stream().map(Faction::getId).toList();
    }
    for (String targetId : oldTargets) {
      factions
          .findByIdAndWorldId(targetId, worldId)
          .ifPresent(
              target -> {
                target.setPower(target.getPower() - old.getPowerDelta());
                target.setWealth(target.getWealth() - old.getWealthDelta());
                target.setManpower(target.getManpower() - old.getManpowerDelta());
                target.setInfluence(target.getInfluence() - old.getInfluenceDelta());
                target.setTerritory(target.getTerritory() - old.getTerritoryDelta());
                factions.save(target);
              });
    }
    old.setTitle(request.title());
    old.setType(request.type());
    old.setCategory(request.category());
    old.setTargetFactionId(request.targetFactionId());
    old.setDetails(request.details());
    return applyEvent(old, request, worldId);
  }

  public void deleteEvent(String worldId, String eventId, String ownerId) {
    get(worldId, ownerId);
    events.delete(
        events
            .findByIdAndWorldId(eventId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Event not found")));
  }

  @Transactional
  public Map<String, Object> simulate(String id, String ownerId) {
    World world = get(id, ownerId);
    List<Faction> available = factions.findByWorldId(id);
    if (available.isEmpty()) {
      String message = "The realm waits for its first faction.";
      logs.save(new SimulationLog(message, world));
      return Map.of("message", message, "factions", available);
    }
    String category = available.size() > 1 && random.nextBoolean() ? "war" : "internal";
    Faction first = available.get(random.nextInt(available.size()));
    Faction second =
        available.size() > 1
            ? available.stream()
                .filter(faction -> !faction.getId().equals(first.getId()))
                .findAny()
                .orElse(first)
            : first;
    int delta = -1 * (5 + random.nextInt(16));
    ApiDtos.EventRequest request =
        new ApiDtos.EventRequest(
            "Random " + category + " event",
            "RANDOM",
            category,
            first.getId(),
            "war".equals(category) ? second.getId() : null,
            0,
            delta,
            delta,
            delta,
            "war".equals(category) ? -5 : 0,
            "The world moved without a player hand.");
    WorldEvent created =
        applyEvent(
            new WorldEvent(
                request.title(),
                request.type(),
                request.category(),
                request.targetFactionId(),
                request.details(),
                world),
            request,
            id);
    return Map.of("message", created.getTitle(), "factions", factions.findByWorldId(id));
  }

  @Transactional
  public World importSnapshot(JsonNode root, String ownerId) {
    JsonNode source = root.has("world") ? root.get("world") : root;
    if (!source.hasNonNull("name") || source.path("name").asText().isBlank()) {
      throw new IllegalArgumentException("Import must contain a world name");
    }
    World world =
        create(
            new ApiDtos.WorldRequest(
                source.path("name").asText(), source.path("description").asText("")),
            ownerId);
    if (root.has("factions") && root.get("factions").isArray()) {
      for (JsonNode faction : root.get("factions")) {
        faction(
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
                faction.path("description").asText("")),
            ownerId);
      }
    }
    if (root.has("events") && root.get("events").isArray()) {
      for (JsonNode event : root.get("events")) {
        event(
            world.getId(),
            new ApiDtos.EventRequest(
                event.path("title").asText("Imported event"),
                event.path("type").asText("IMPORTED"),
                event.path("category").asText("internal"),
                null,
                null,
                event.path("powerDelta").asInt(),
                event.path("wealthDelta").asInt(),
                event.path("manpowerDelta").asInt(),
                event.path("influenceDelta").asInt(),
                event.path("territoryDelta").asInt(),
                event.path("details").asText("")),
            ownerId);
      }
    }
    return world;
  }

  public Map<String, Object> data(String id, String ownerId) {
    get(id, ownerId);
    return Map.of(
        "factions",
        factions.findByWorldId(id),
        "events",
        events.findByWorldIdOrderByOccurredAtDesc(id),
        "logs",
        logs.findTop100ByWorldIdOrderByCreatedAtDesc(id));
  }
}
