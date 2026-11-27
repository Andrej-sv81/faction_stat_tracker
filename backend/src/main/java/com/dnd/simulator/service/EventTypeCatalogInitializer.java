package com.dnd.simulator.service;

import com.dnd.simulator.entity.EventType;
import com.dnd.simulator.repository.EventTypeRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventTypeCatalogInitializer implements ApplicationRunner {
  private static final String CATALOG_RESOURCE = "faction_event_categories.csv";

  private final EventTypeRepository eventTypes;

  public EventTypeCatalogInitializer(EventTypeRepository eventTypes) {
    this.eventTypes = eventTypes;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    Set<String> existingCodes =
        eventTypes.findAll().stream().map(EventType::getCode).collect(Collectors.toSet());
    List<EventType> missingTypes = new ArrayList<>();

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource(CATALOG_RESOURCE).getInputStream(),
                StandardCharsets.UTF_8))) {
      String header = reader.readLine();
      if (!"domain,category,description".equals(header)) {
        throw new IllegalStateException("Unexpected event type catalog header");
      }

      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        List<String> fields = parseCsvLine(line, lineNumber);
        if (fields.size() != 3 || fields.stream().anyMatch(String::isBlank)) {
          throw new IllegalStateException(
              "Invalid event type catalog row at line " + lineNumber);
        }

        String domain = fields.get(0).trim();
        String code = fields.get(1).trim().toUpperCase(Locale.ROOT);
        if (existingCodes.add(code)) {
          missingTypes.add(
              new EventType(code, displayName(code), domain, fields.get(2).trim()));
        }
      }
    }

    eventTypes.saveAll(missingTypes);
  }

  private List<String> parseCsvLine(String line, int lineNumber) {
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;

    for (int index = 0; index < line.length(); index++) {
      char current = line.charAt(index);
      if (current == '"') {
        if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          field.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (current == ',' && !quoted) {
        fields.add(field.toString());
        field.setLength(0);
      } else {
        field.append(current);
      }
    }

    if (quoted) {
      throw new IllegalStateException("Unclosed quoted value in event catalog line " + lineNumber);
    }
    fields.add(field.toString());
    return fields;
  }

  private String displayName(String code) {
    StringBuilder name = new StringBuilder();
    for (String word : code.toLowerCase(Locale.ROOT).split("_")) {
      if (!name.isEmpty()) {
        name.append(' ');
      }
      name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return name.toString();
  }
}
