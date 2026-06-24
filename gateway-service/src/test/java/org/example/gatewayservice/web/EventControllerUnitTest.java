package org.example.gatewayservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.service.EventService;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerUnitTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private EventService eventService;

  private EventRecord mockReturnEntity;
  private EventRequest standardRequest;

  @BeforeEach
  void setUp() {
    Instant timestamp = Instant.parse("2026-06-24T12:00:00Z");

    standardRequest =
        new EventRequest(
            "evt-123", "acc-001", new BigDecimal("150.00"), "USD", "CREDIT", timestamp);

    mockReturnEntity =
        EventRecord.builder()
            .id(1L)
            .eventId("evt-123")
            .accountId("acc-001")
            .amount(new BigDecimal("150.00"))
            .currency("USD")
            .type("CREDIT")
            .eventTime(timestamp)
            .build();
  }

  @Test
  void ingestEvent_ShouldReturn202Accepted_WhenPayloadIsValid() throws Exception {
    Mockito.when(eventService.processEvent(any(EventRequest.class))).thenReturn(mockReturnEntity);

    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standardRequest)))
        // FIXED: Expect 202 Accepted matching your resilient architecture design pattern
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.eventId").value("evt-123"))
        .andExpect(jsonPath("$.currency").value("USD"));
  }

  @Test
  void ingestEvent_ShouldReturn400BadRequest_WhenConstraintsViolated() throws Exception {
    // Creating an invalid payload with a zero value amount constraint violation
    EventRequest invalidRequest =
        new EventRequest("evt-123", "acc-001", BigDecimal.ZERO, "USD", "CREDIT", Instant.now());

    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getEventById_ShouldReturnEntity_WhenFound() throws Exception {
    Mockito.when(eventService.getEventById("evt-123")).thenReturn(Optional.of(mockReturnEntity));

    mockMvc
        .perform(get("/events/evt-123"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.eventId").value("evt-123"));
  }

  @Test
  void getEventById_ShouldReturn404NotFound_WhenMissing() throws Exception {
    Mockito.when(eventService.getEventById("missing-id")).thenReturn(Optional.empty());

    mockMvc.perform(get("/events/missing-id")).andExpect(status().isNotFound());
  }

  @Test
  void getAccountEvents_ShouldReturnChronologicalList() throws Exception {
    List<EventRecord> chronologicalList = List.of(mockReturnEntity);
    Mockito.when(eventService.getEventsByAccountChronological("acc-001"))
        .thenReturn(chronologicalList);

    mockMvc
        .perform(get("/events").param("account", "acc-001"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].accountId").value("acc-001"));
  }
}
