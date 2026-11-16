package com.dnd.simulator.repository;
import com.dnd.simulator.entity.WorldEvent; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WorldEventRepository extends JpaRepository<WorldEvent,String>{List<WorldEvent> findByWorldIdOrderByOccurredAtDesc(String worldId); Optional<WorldEvent> findByIdAndWorldId(String id,String worldId);}
