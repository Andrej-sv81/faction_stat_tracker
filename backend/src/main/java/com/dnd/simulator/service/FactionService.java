package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.Faction;
import com.dnd.simulator.entity.World;
import com.dnd.simulator.repository.FactionRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Service
public class FactionService {
  private final WorldService worlds;
  private final FactionRepository factions;

  public FactionService(WorldService worlds, FactionRepository factions) {
    this.worlds = worlds;
    this.factions = factions;
  }

  @Transactional
  public Faction createFaction(
      String worldId, ApiDtos.FactionRequest request, String ownerId) {
    World world = worlds.get(worldId, ownerId);
    Faction faction =
        new Faction(
            request.name(), request.alignment(), request.power(), request.description(), world);
    return factions.save(fill(faction, request));
  }

  @Transactional
  public Faction updateFaction(
      String worldId, String factionId, ApiDtos.FactionRequest request, String ownerId) {
    worlds.get(worldId, ownerId);
    Faction faction =
        factions
            .findByIdAndWorldId(factionId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Faction not found"));
    if (request.version() == null) {
      throw new IllegalArgumentException("A version is required to update a faction");
    }
    if (!request.version().equals(faction.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(Faction.class, factionId);
    }
    return factions.save(fill(faction, request));
  }

  @Transactional
  public void deleteFaction(String worldId, String factionId, String ownerId) {
    worlds.get(worldId, ownerId);
    factions.delete(
        factions
            .findByIdAndWorldId(factionId, worldId)
            .orElseThrow(() -> new NoSuchElementException("Faction not found")));
  }

  @Transactional(readOnly = true)
  public List<Faction> findByWorldId(String worldId) {
    return factions.findByWorldId(worldId);
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
}
