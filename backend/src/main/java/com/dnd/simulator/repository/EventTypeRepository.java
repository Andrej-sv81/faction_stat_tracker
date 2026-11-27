package com.dnd.simulator.repository;

import com.dnd.simulator.entity.EventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventType, String> {
  List<EventType> findByEnabledTrueOrderByDomainAscNameAsc();

  Optional<EventType> findByCodeAndEnabledTrue(String code);
}
