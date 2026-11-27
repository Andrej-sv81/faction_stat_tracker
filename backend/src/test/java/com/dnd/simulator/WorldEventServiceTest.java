package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.dnd.simulator.service.WorldSimulationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorldEventServiceTest {
  @Test
  void eventRequestUsesStableCatalogCodeWithoutDuplicatedDomain() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ApiDtos.EventRequest request =
        mapper.readValue(
            """
                {
                  "title": "A pact is signed",
                  "type": "ALLIANCE",
                  "targetFactionId": "faction-1",
                  "severity": 4,
                  "powerDelta": 0,
                  "wealthDelta": 5,
                  "manpowerDelta": 0,
                  "influenceDelta": 3,
                  "territoryDelta": 0
                }
                """,
            ApiDtos.EventRequest.class);

    assertEquals("ALLIANCE", request.type());
    assertEquals(4, request.severity());
  }

  @Test
  void eventSerializationKeepsTypeAsCodeAndDerivesItsCategory() throws Exception {
    EventType type =
        new EventType(
            "ALLIANCE",
            "Alliance",
            "Politics, Diplomacy & Leadership",
            "A political pact.");
    WorldEvent event =
        new WorldEvent("A pact is signed", type, "faction-1", "A political pact.", mock(World.class));

    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    JsonNode json = mapper.readTree(mapper.writeValueAsString(event));

    assertEquals("ALLIANCE", json.path("type").asText());
    assertEquals("Politics, Diplomacy & Leadership", json.path("category").asText());
  }

  @Test
  void storesTheRequestedSeverityOnCreatedEvents() {
    WorldService worlds = mock(WorldService.class);
    FactionRepository factions = mock(FactionRepository.class);
    WorldEventRepository events = mock(WorldEventRepository.class);
    EventTypeRepository eventTypes = mock(EventTypeRepository.class);
    SimulationLogRepository logs = mock(SimulationLogRepository.class);
    World world = mock(World.class);
    EventType type =
        new EventType("DISCOVERY", "Discovery", "Knowledge", "An artifact is found.");
    when(world.getId()).thenReturn("world-1");
    when(eventTypes.findByCodeAndEnabledTrue("DISCOVERY")).thenReturn(Optional.of(type));
    when(factions.findByWorldId("world-1")).thenReturn(List.of());
    when(events.save(any(WorldEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    WorldEventService service =
        new WorldEventService(worlds, factions, events, eventTypes, logs);
    ApiDtos.EventRequest request =
        new ApiDtos.EventRequest(
            "A discovery",
            "DISCOVERY",
            null,
            null,
            0,
            0,
            0,
            0,
            0,
            "An artifact is found.",
            4,
            null);

    WorldEvent created = service.createForWorld(world, request);

    assertEquals(4, created.getSeverity());
  }

  @Test
  void randomSimulationUsesAnEnabledDatabaseCatalogType() {
    WorldService worlds = mock(WorldService.class);
    FactionRepository factionRepository = mock(FactionRepository.class);
    WorldEventRepository eventRepository = mock(WorldEventRepository.class);
    EventTypeRepository eventTypeRepository = mock(EventTypeRepository.class);
    SimulationLogRepository logs = mock(SimulationLogRepository.class);
    World world = mock(World.class);
    Faction faction = mock(Faction.class);
    EventType type =
        new EventType(
            "ALLIANCE",
            "Alliance",
            "Politics, Diplomacy & Leadership",
            "A political pact.");
    when(worlds.get("world-1", "user-1")).thenReturn(world);
    when(world.getId()).thenReturn("world-1");
    when(factionRepository.findByWorldId("world-1")).thenReturn(List.of(faction));
    when(factionRepository.findByIdAndWorldId("faction-1", "world-1"))
        .thenReturn(Optional.of(faction));
    when(faction.getId()).thenReturn("faction-1");
    when(faction.getWorld()).thenReturn(world);
    when(faction.getPower()).thenReturn(50);
    when(faction.getWealth()).thenReturn(50);
    when(faction.getManpower()).thenReturn(50);
    when(faction.getInfluence()).thenReturn(50);
    when(faction.getTerritory()).thenReturn(50);
    when(eventTypeRepository.findByEnabledTrueOrderByDomainAscNameAsc())
        .thenReturn(List.of(type));
    when(eventTypeRepository.findByCodeAndEnabledTrue("ALLIANCE")).thenReturn(Optional.of(type));
    when(eventRepository.save(any(WorldEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    FactionService factions = new FactionService(worlds, factionRepository);
    WorldEventService events =
        new WorldEventService(
            worlds, factionRepository, eventRepository, eventTypeRepository, logs);
    WorldSimulationService simulations =
        new WorldSimulationService(worlds, factions, events, logs);

    simulations.simulate("world-1", "user-1");

    var eventCaptor = org.mockito.ArgumentCaptor.forClass(WorldEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    WorldEvent savedEvent = eventCaptor.getValue();
    assertEquals("ALLIANCE", savedEvent.getType().getCode());
    assertEquals("Politics, Diplomacy & Leadership", savedEvent.getCategory());
    assertEquals("faction-1", savedEvent.getTargetFactionId());
    assertNull(savedEvent.getSecondTargetFactionId());
    assertEquals(3, savedEvent.getSeverity());
  }

  @Test
  void rejectsUnknownOrDisabledCatalogType() {
    WorldService worlds = mock(WorldService.class);
    FactionRepository factions = mock(FactionRepository.class);
    WorldEventRepository events = mock(WorldEventRepository.class);
    EventTypeRepository eventTypes = mock(EventTypeRepository.class);
    SimulationLogRepository logs = mock(SimulationLogRepository.class);
    when(worlds.get("world-1", "user-1")).thenReturn(mock(World.class));
    when(eventTypes.findByCodeAndEnabledTrue("MISSING")).thenReturn(Optional.empty());
    WorldEventService service =
        new WorldEventService(worlds, factions, events, eventTypes, logs);
    ApiDtos.EventRequest request =
        new ApiDtos.EventRequest(
            "An unknown event",
            "MISSING",
            null,
            null,
            0,
            0,
            0,
            0,
            0,
            "No catalog type exists.",
            null,
            null);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.createEvent("world-1", request, "user-1"));

    verifyNoInteractions(factions, events, logs);
  }
}
