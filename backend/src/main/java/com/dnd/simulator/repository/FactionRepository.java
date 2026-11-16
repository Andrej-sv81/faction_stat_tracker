package com.dnd.simulator.repository;
import com.dnd.simulator.entity.Faction; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface FactionRepository extends JpaRepository<Faction,String>{List<Faction> findByWorldId(String worldId); Optional<Faction> findByIdAndWorldId(String id,String worldId);}
