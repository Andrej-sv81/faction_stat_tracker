package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.entity.World;
import com.dnd.simulator.service.WorldService;
import com.dnd.simulator.service.WorldSimulationScheduler;
import com.dnd.simulator.service.WorldSimulationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorldSimulationSchedulerTest {
  private WorldService worlds;
  private WorldSimulationService simulations;
  private WorldSimulationScheduler scheduler;

  @BeforeEach
  void setUp() {
    worlds = mock(WorldService.class);
    simulations = mock(WorldSimulationService.class);
    when(worlds.get("world-1", "user-1")).thenReturn(mock(World.class));
    scheduler = new WorldSimulationScheduler(worlds, simulations);
  }

  @AfterEach
  void shutDownScheduler() {
    scheduler.shutdown();
  }

  @Test
  void startsOnlyOneSchedulePerWorldAndStopsIt() {
    ApiDtos.SimulationStatusResponse started =
        scheduler.start(
            "world-1", "user-1", new ApiDtos.SimulationControlRequest(5, 2));
    ApiDtos.SimulationStatusResponse duplicate =
        scheduler.start(
            "world-1", "user-1", new ApiDtos.SimulationControlRequest(1, 5));

    assertTrue(started.running());
    assertTrue(duplicate.running());
    assertEquals(5, duplicate.intervalMinutes());
    assertEquals(2, duplicate.severity());

    ApiDtos.SimulationStatusResponse stopped = scheduler.stop("world-1", "user-1");

    assertFalse(stopped.running());
    assertEquals(5, stopped.intervalMinutes());
    assertEquals(2, stopped.severity());
    assertFalse(scheduler.status("world-1", "user-1").running());
    verify(worlds, times(4)).get("world-1", "user-1");
  }

  @Test
  void rejectsInvalidIntervalAndSeverity() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scheduler.start(
                "world-1", "user-1", new ApiDtos.SimulationControlRequest(0, 3)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scheduler.start(
                "world-1", "user-1", new ApiDtos.SimulationControlRequest(5, 6)));

    assertFalse(scheduler.status("world-1", "user-1").running());
  }
}
