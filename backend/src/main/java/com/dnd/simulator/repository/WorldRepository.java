package com.dnd.simulator.repository;

import com.dnd.simulator.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldRepository extends JpaRepository<World, String> {
  List<World> findByOwnerEmailOrderByCreatedAtDesc(String email);

  Optional<World> findByIdAndOwnerEmail(String id, String email);
}
