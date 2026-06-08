package org.example.gatewayservice.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.service.EventService;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event ingestion and forwarding APIs")
public class EventController {

  private final EventService eventService;

  @PostMapping
  @Operation(
      summary = "Ingest a financial event",
      description =
          "Accepts an event, stores it idempotently, and forwards it to the Account Service.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Event successfully processed",
            content = @Content(schema = @Schema(implementation = EventRecord.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public ResponseEntity<EventRecord> ingestEvent(@RequestBody @Valid EventRequest request) {
    return ResponseEntity.ok(eventService.processEvent(request));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Retrieve a single event by its ID")
  public ResponseEntity<EventRecord> getEventById(@PathVariable String id) {
    return eventService
        .getEventById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping
  @Operation(summary = "List events for an account, ordered chronologically")
  public ResponseEntity<List<EventRecord>> getAccountEvents(@RequestParam String account) {
    return ResponseEntity.ok(eventService.getEventsByAccountChronological(account));
  }
}
