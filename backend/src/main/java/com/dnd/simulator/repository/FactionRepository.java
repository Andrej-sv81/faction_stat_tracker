package com.dnd.simulator.repository;

import com.dnd.simulator.entity.Faction;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactionRepository extends JpaRepository<Faction, String> {
  List<Faction> findByWorldId(String worldId);

  Optional<Faction> findByIdAndWorldId(String id, String worldId);
}
