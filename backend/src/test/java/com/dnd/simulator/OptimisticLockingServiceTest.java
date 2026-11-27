package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.EventType;
import com.dnd.simulator.entity.Faction;
import com.dnd.simulator.entity.World;
import com.dnd.simulator.entity.WorldEvent;
import com.dnd.simulator.repository.EventTypeRepository;
import com.dnd.simulator.repository.FactionRepository;
import com.dnd.simulator.repository.SimulationLogRepository;
import com.dnd.simulator.repository.WorldEventRepository;
import com.dnd.simulator.service.FactionService;
import com.dnd.simulator.service.WorldEventService;
import com.dnd.simulator.service.WorldService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class OptimisticLockingServiceTest {
  @Test
  void rejectsFactionUpdateWhenVersionIsStale() {
    WorldService worlds = mock(WorldService.class);
    FactionRepository factions = mock(FactionRepository.class);
    Faction faction = mock(Faction.class);
    when(worlds.get("world-1", "user-1")).thenReturn(mock(World.class));
    when(factions.findByIdAndWorldId("faction-1", "world-1"))
        .thenReturn(Optional.of(faction));
    when(faction.getVersion()).thenReturn(2L);
    ApiDtos.FactionRequest request =
        new ApiDtos.FactionRequest(
            "Faction",
            "Neutral",
            50,
            50,
            50,
            50,
            50,
            null,
            "Description",
            1L);
    FactionService service = new FactionService(worlds, factions);

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> service.updateFaction("world-1", "faction-1", request, "user-1"));

    verify(factions, never()).save(any(Faction.class));
  }

  @Test
  void rejectsEventUpdateWhenVersionIsStale() {
    WorldService worlds = mock(WorldService.class);
    FactionRepository factions = mock(FactionRepository.class);
    WorldEventRepository events = mock(WorldEventRepository.class);
    EventTypeRepository eventTypes = mock(EventTypeRepository.class);
    SimulationLogRepository logs = mock(SimulationLogRepository.class);
    WorldEvent event = mock(WorldEvent.class);
    EventType type = new EventType("ALLIANCE", "Alliance", "Politics", "A pact.");
    when(worlds.get("world-1", "user-1")).thenReturn(mock(World.class));
    when(eventTypes.findByCodeAndEnabledTrue("ALLIANCE")).thenReturn(Optional.of(type));
    when(events.findByIdAndWorldId("event-1", "world-1")).thenReturn(Optional.of(event));
    when(event.getVersion()).thenReturn(3L);
    ApiDtos.EventRequest request =
        new ApiDtos.EventRequest(
            "An alliance",
            "ALLIANCE",
            null,
            null,
            0,
            0,
            0,
            0,
            0,
            "A political pact.",
            2L);
    WorldEventService service =
        new WorldEventService(worlds, factions, events, eventTypes, logs);

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> service.updateEvent("world-1", "event-1", request, "user-1"));

    verifyNoInteractions(factions, logs);
  }
}
