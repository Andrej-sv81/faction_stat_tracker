package com.dnd.simulator.repository;
import com.dnd.simulator.entity.SimulationLog; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SimulationLogRepository extends JpaRepository<SimulationLog,String>{List<SimulationLog> findTop100ByWorldIdOrderByCreatedAtDesc(String worldId);}
