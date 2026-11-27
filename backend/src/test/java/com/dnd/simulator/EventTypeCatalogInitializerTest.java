package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dnd.simulator.entity.EventType;
import com.dnd.simulator.repository.EventTypeRepository;
import com.dnd.simulator.service.EventTypeCatalogInitializer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class EventTypeCatalogInitializerTest {
  @Test
  void seedsMissingCatalogTypesAndParsesQuotedCommas() throws Exception {
    EventTypeRepository repository = mock(EventTypeRepository.class);
    when(repository.findAll())
        .thenReturn(
            List.of(
                new EventType(
                    "ALLIANCE", "Custom Alliance", "Custom Domain", "Existing catalog entry.")));
    List<EventType> savedTypes = new ArrayList<>();
    when(repository.saveAll(any()))
        .thenAnswer(
            invocation -> {
              Iterable<EventType> types = invocation.getArgument(0);
              types.forEach(savedTypes::add);
              return savedTypes;
            });

    new EventTypeCatalogInitializer(repository).run(new DefaultApplicationArguments());

    assertEquals(29, savedTypes.size());
    EventType leadershipChange =
        savedTypes.stream()
            .filter(type -> type.getCode().equals("CHANGE_OF_LEADERSHIP"))
            .findFirst()
            .orElseThrow();
    assertEquals("Politics, Diplomacy & Leadership", leadershipChange.getDomain());
    assertEquals(
        "Succession, assassination, coup, election, or abdication.",
        leadershipChange.getDescription());
    assertFalse(savedTypes.stream().anyMatch(type -> type.getCode().equals("ALLIANCE")));
  }
}
