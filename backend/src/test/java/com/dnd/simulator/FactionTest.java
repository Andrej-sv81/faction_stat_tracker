package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dnd.simulator.entity.Faction;
import org.junit.jupiter.api.Test;

class FactionTest {
  @Test
  void territoryIsKeptWithinWorldPercentageBounds() {
    Faction faction = new Faction();

    faction.setTerritory(140);
    assertEquals(100, faction.getTerritory());

    faction.setTerritory(-10);
    assertEquals(0, faction.getTerritory());
  }
}
