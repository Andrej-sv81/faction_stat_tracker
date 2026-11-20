package com.dnd.simulator.repository;

import com.dnd.simulator.entity.SimulationLog;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationLogRepository extends JpaRepository<SimulationLog, String> {
  List<SimulationLog> findTop100ByWorldIdOrderByCreatedAtDesc(String worldId);
}
