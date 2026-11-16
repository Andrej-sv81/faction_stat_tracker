package com.dnd.simulator;

import com.dnd.simulator.entity.Faction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
