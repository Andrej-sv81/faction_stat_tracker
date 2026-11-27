package com.dnd.simulator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomEventImpactTest {
  @Test
  void allianceImprovesManpowerAndInfluence() {
    RandomEventImpact.StatDeltas deltas =
        RandomEventImpact.forEvent("ALLIANCE", "Politics, Diplomacy & Leadership", 8, new Random(1));

    assertEquals(0, deltas.powerDelta());
    assertEquals(0, deltas.wealthDelta());
    assertEquals(8, deltas.manpowerDelta());
    assertEquals(8, deltas.influenceDelta());
    assertEquals(0, deltas.territoryDelta());
  }

  @Test
  void monetaryGainIncreasesWealth() {
    RandomEventImpact.StatDeltas deltas =
        RandomEventImpact.forEvent("MONETARY_GAIN", "Economy & Resources", 12, new Random(1));

    assertEquals(12, deltas.wealthDelta());
    assertEquals(0, deltas.manpowerDelta());
    assertEquals(0, deltas.influenceDelta());
  }

  @Test
  void conflictReducesWealthManpowerAndPower() {
    RandomEventImpact.StatDeltas deltas =
        RandomEventImpact.forEvent("CONFLICT", "Warfare & Security", 10, new Random(1));

    assertTrue(deltas.wealthDelta() < 0);
    assertTrue(deltas.manpowerDelta() < 0);
    assertTrue(deltas.powerDelta() < 0);
    assertEquals(0, deltas.influenceDelta());
  }

  @Test
  void ambiguousMigrationCanRepresentAnInfluxOrAnExodus() {
    RandomEventImpact.StatDeltas deltas =
        RandomEventImpact.forEvent("MIGRATION", "Environment & Society", 7, new Random(1));

    assertTrue(
        (deltas.manpowerDelta() > 0 && deltas.wealthDelta() > 0)
            || (deltas.manpowerDelta() < 0 && deltas.wealthDelta() < 0));
  }

  @Test
  void unknownTypesUseTheirDomainForAReasonableFallback() {
    RandomEventImpact.StatDeltas deltas =
        RandomEventImpact.forEvent("NEW_ECONOMIC_EVENT", "Economy & Resources", 6, new Random(1));

    assertEquals(6, deltas.wealthDelta());
    assertEquals(0, deltas.powerDelta());
    assertEquals(0, deltas.manpowerDelta());
  }
}
