package com.dnd.simulator.controller;

import com.dnd.simulator.dto.ApiDtos;
import com.dnd.simulator.service.WorldEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-types")
public class EventTypeController {
  private final WorldEventService events;

  public EventTypeController(WorldEventService events) {
    this.events = events;
  }

  @GetMapping
  public List<ApiDtos.EventTypeResponse> list() {
    return events.eventTypes();
  }
}
