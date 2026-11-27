package com.dnd.simulator.service;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

final class RandomEventImpact {
  private static final Map<String, Profile> PROFILES =
      Map.ofEntries(
          Map.entry("CHANGE_OF_LEADERSHIP", new Profile(-1, 0, 0, -1, 0)),
          Map.entry("ALLIANCE", new Profile(0, 0, 1, 1, 0)),
          Map.entry("BETRAYAL", new Profile(-1, 0, 0, -1, 0)),
          Map.entry("DIPLOMATIC_INCIDENT", new Profile(0, -1, 0, -1, 0)),
          Map.entry("ANNEXATION", new Profile(0, -1, 1, 0, 1)),
          Map.entry("SCHISM", new Profile(0, 0, -1, -1, 0)),
          Map.entry("REBELLION", new Profile(-1, -1, -1, 0, 0)),
          Map.entry("CONFLICT", new Profile(-1, -1, -1, 0, 0)),
          Map.entry("ESPIONAGE", new Profile(1, -1, 0, 1, 0)),
          Map.entry("FORTIFICATION", new Profile(1, -1, 0, 0, 0)),
          Map.entry("PIRACY_OR_BANDITRY", new Profile(0, -1, -1, 0, 0)),
          Map.entry("MERCENARY_ACTION", new Profile(1, -1, 1, 0, 0)),
          Map.entry("MONETARY_GAIN", new Profile(0, 1, 0, 0, 0)),
          Map.entry("FINANCIAL_CRISIS", new Profile(0, -1, 0, -1, 0)),
          Map.entry("CONTRACT", new Profile(0, 1, 0, 1, 0)),
          Map.entry("RESOURCE_BOOM", new Profile(0, 1, 1, 0, 0)),
          Map.entry("SCARCITY", new Profile(0, -1, -1, 0, 0)),
          Map.entry("INFRASTRUCTURE_PROJECT", new Profile(1, -1, 0, 0, 1)),
          Map.entry("DISCOVERY", new Profile(0, 0, 0, 1, 1)),
          Map.entry("ARCANE_ANOMALY", new Profile(-1, 0, 0, 0, -1)),
          Map.entry("INNOVATION", new Profile(1, 1, 0, 0, 0)),
          Map.entry("HERESY_OR_PROPHECY", new Profile(-1, 0, 0, 1, 0)),
          Map.entry("NATURAL_DISASTER", new Profile(0, -1, -1, 0, -1)),
          Map.entry("PLAGUE", new Profile(0, -1, -1, 0, 0)),
          Map.entry("MIGRATION", new Profile(0, 1, 1, 0, 0)),
          Map.entry("CULTURAL_RENAISSANCE", new Profile(0, 1, 0, 1, 0)),
          Map.entry("CRIME_WAVE", new Profile(0, -1, 0, -1, 0)),
          Map.entry("MONSTER_INCURSION", new Profile(-1, 0, -1, 0, -1)),
          Map.entry("DIVINE_INTERVENTION", new Profile(1, 0, 0, 1, 0)),
          Map.entry("RELIC_AWAKENING", new Profile(1, -1, 0, 1, 0)));

  private RandomEventImpact() {}

  static StatDeltas forEvent(String code, String domain, int magnitude, Random random) {
    String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
    Profile profile = PROFILES.getOrDefault(normalizedCode, fallbackProfile(domain));
    if (normalizedCode.equals("MIGRATION") || normalizedCode.equals("DIVINE_INTERVENTION")) {
      int direction = random.nextBoolean() ? 1 : -1;
      profile = profile.multiply(direction);
    }
    return new StatDeltas(
        profile.power() * magnitude,
        profile.wealth() * magnitude,
        profile.manpower() * magnitude,
        profile.influence() * magnitude,
        profile.territory() * magnitude);
  }

  private static Profile fallbackProfile(String domain) {
    String normalizedDomain = domain == null ? "" : domain.toLowerCase(Locale.ROOT);
    if (normalizedDomain.contains("warfare") || normalizedDomain.contains("threat")) {
      return new Profile(0, -1, -1, 0, 0);
    }
    if (normalizedDomain.contains("economy")) {
      return new Profile(0, 1, 0, 0, 0);
    }
    if (normalizedDomain.contains("environment")) {
      return new Profile(0, -1, -1, 0, 0);
    }
    if (normalizedDomain.contains("knowledge")) {
      return new Profile(1, 0, 0, 1, 0);
    }
    return new Profile(0, 0, 0, 1, 0);
  }

  record StatDeltas(
      int powerDelta,
      int wealthDelta,
      int manpowerDelta,
      int influenceDelta,
      int territoryDelta) {
    String summary() {
      StringBuilder result = new StringBuilder();
      append(result, "Power", powerDelta);
      append(result, "Wealth", wealthDelta);
      append(result, "Manpower", manpowerDelta);
      append(result, "Influence", influenceDelta);
      append(result, "Territory", territoryDelta);
      return result.toString();
    }

    private static void append(StringBuilder result, String name, int delta) {
      if (delta == 0) {
        return;
      }
      if (!result.isEmpty()) {
        result.append(", ");
      }
      result.append(name).append(' ').append(String.format("%+d", delta));
      if (name.equals("Territory")) {
        result.append('%');
      }
    }
  }

  private record Profile(int power, int wealth, int manpower, int influence, int territory) {
    private Profile multiply(int factor) {
      return new Profile(
          power * factor, wealth * factor, manpower * factor, influence * factor, territory * factor);
    }
  }
}
