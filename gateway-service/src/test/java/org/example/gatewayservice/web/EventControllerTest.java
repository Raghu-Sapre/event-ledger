package org.example.gatewayservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.service.EventService;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  // Use the official non-deprecated Spring Boot 3.4+ Mockito registry overrides
  @MockitoBean private EventService eventService;

  private EventRequest validRequestRecord;
  private EventRecord mockReturnEntity;

  @BeforeEach
  void setUp() {
    // Instantiate using your record's immutable canonical constructor
    validRequestRecord =
        new EventRequest("evt-9999", "acc-123", new BigDecimal("150.00"), "CREDIT");

    mockReturnEntity = new EventRecord();
    mockReturnEntity.setId(1L);
    mockReturnEntity.setAccountId("acc-123");
    mockReturnEntity.setAmount(new BigDecimal("150.00"));
    mockReturnEntity.setEventId("evt-9999");
    mockReturnEntity.setType("CREDIT");
    mockReturnEntity.setEventTime(ZonedDateTime.now().toInstant());
  }

  @Test
  void ingestEvent_ShouldReturnOk_WhenRecordIsValid() throws Exception {
    // Given
    Mockito.when(eventService.processEvent(any(EventRequest.class))).thenReturn(mockReturnEntity);

    // When & Then
    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestRecord)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // <-- FIXED LINE
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.accountId").value("acc-123"))
        .andExpect(jsonPath("$.eventId").value("evt-9999"))
        .andExpect(jsonPath("$.amount").value(150.00));
  }

  @Test
  void ingestEvent_ShouldReturnBadRequest_WhenValidationConstraintsFail() throws Exception {
    // Passing an invalid balance (0.00 fails @DecimalMin("0.01") constraint rule)
    EventRequest invalidRequestRecord =
        new EventRequest("evt-9999", "acc-123", new BigDecimal("0.00"), "CREDIT");

    // When & Then
    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestRecord)))
        .andExpect(status().isBadRequest());
  }
}
