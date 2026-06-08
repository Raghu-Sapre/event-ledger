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

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private EventService eventService;

  private EventRequest validRequestRecord;
  private EventRecord mockReturnEntity;

  @BeforeEach
  void setUp() {
    Instant specificTime = Instant.parse("2026-05-15T14:02:11Z");

    // Canonical constructor reflecting full assignment properties (eventId, accountId, amount, currency, type, eventTimestamp)
    validRequestRecord = new EventRequest("evt-9999", "acc-123", new BigDecimal("150.00"), "USD", "CREDIT", specificTime);

    mockReturnEntity = EventRecord.builder()
            .id(1L)
            .eventId("evt-9999")
            .accountId("acc-123")
            .amount(new BigDecimal("150.00"))
            .currency("USD")
            .type("CREDIT")
            .eventTime(specificTime)
            .build();
  }

  @Test
  void ingestEvent_ShouldReturnOk_WhenRecordIsValid() throws Exception {
    Mockito.when(eventService.processEvent(any(EventRequest.class))).thenReturn(mockReturnEntity);

    mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequestRecord)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.accountId").value("acc-123"))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.eventId").value("evt-9999"));
  }

  @Test
  void ingestEvent_ShouldReturnBadRequest_WhenValidationConstraintsFail() throws Exception {
    // Passing an invalid balance (0.00 violates @DecimalMin("0.01"))
    EventRequest invalidRequestRecord = new EventRequest("evt-9999", "acc-123", new BigDecimal("0.00"), "USD", "CREDIT", Instant.now());

    mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequestRecord)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void getEventById_ShouldReturnEntity_WhenFound() throws Exception {
    Mockito.when(eventService.getEventById("evt-9999")).thenReturn(Optional.of(mockReturnEntity));

    mockMvc.perform(get("/events/evt-9999"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.eventId").value("evt-9999"));
  }

  @Test
  void getEventById_ShouldReturn404_WhenNotFound() throws Exception {
    Mockito.when(eventService.getEventById("missing-id")).thenReturn(Optional.empty());

    mockMvc.perform(get("/events/missing-id"))
            .andExpect(status().isNotFound());
  }

  @Test
  void getAccountEvents_ShouldReturnChronologicalList() throws Exception {
    List<EventRecord> chronologicalList = List.of(mockReturnEntity);
    Mockito.when(eventService.getEventsByAccountChronological("acc-123")).thenReturn(chronologicalList);

    mockMvc.perform(get("/events").param("account", "acc-123"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].accountId").value("acc-123"));
  }
}