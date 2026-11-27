package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.World;
import com.dnd.simulator.repository.AppUserRepository;
import com.dnd.simulator.repository.WorldRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldService {
  private final WorldRepository worlds;
  private final AppUserRepository users;

  public WorldService(WorldRepository worlds, AppUserRepository users) {
    this.worlds = worlds;
    this.users = users;
  }

  public List<World> list(String ownerId) {
    return worlds.findByOwnerIdOrderByCreatedAtDesc(ownerId);
  }

  public World get(String id, String ownerId) {
    return worlds
        .findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new NoSuchElementException("World not found"));
  }

  @Transactional
  public World create(ApiDtos.WorldRequest request, String ownerId) {
    return worlds.save(
        new World(
            request.name(),
            request.description(),
            users.findById(ownerId).orElseThrow()));
  }
}
