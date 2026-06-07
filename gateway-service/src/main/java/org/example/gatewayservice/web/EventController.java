package org.example.gatewayservice.web;

import lombok.RequiredArgsConstructor;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.service.EventService;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

  private final EventService eventService;

  @PostMapping
  public ResponseEntity<EventRecord> createEvent(@RequestBody EventRequest request) {
    EventRecord eventRecord = eventService.processEvent(request);
    return ResponseEntity.ok(eventRecord);
  }
}
