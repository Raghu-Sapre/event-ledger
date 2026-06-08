package org.example.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.repository.EventRecordRepository;
import org.example.gatewayservice.web.AccountClient;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRecordRepository eventRecordRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private EventService eventService;

    private EventRequest request;
    private EventRecord existingRecord;

    @BeforeEach
    void setUp() {
        request = new EventRequest("evt-123", "acc-456", new BigDecimal("100.00"), "CREDIT");

        existingRecord = EventRecord.builder()
                .id(1L)
                .eventId("evt-123")
                .accountId("acc-456")
                .amount(new BigDecimal("100.00"))
                .type("CREDIT")
                .build();
    }

    @Test
    void processEvent_ShouldSaveAndReturn_WhenEventIsNew() {
        // Given
        when(eventRecordRepository.findByEventId(request.eventId())).thenReturn(Optional.empty());
        when(eventRecordRepository.save(any(EventRecord.class))).thenAnswer(invocation -> {
            EventRecord savedEntity = invocation.getArgument(0);
            savedEntity.setId(99L); // Mock assigning an auto-generated DB ID
            return savedEntity;
        });

        // When
        EventRecord result = eventService.processEvent(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getEventId()).isEqualTo("evt-123");

        // Verify downstream account client and repository interactions occurred exactly once
        verify(accountClient, times(1)).applyEvent(request);
        verify(eventRecordRepository, times(1)).save(any(EventRecord.class));
    }

    @Test
    void processEvent_ShouldReturnExistingRecord_WhenEventIsDuplicate_Idempotency() {
        // Given
        when(eventRecordRepository.findByEventId(request.eventId())).thenReturn(Optional.of(existingRecord));

        // When
        EventRecord result = eventService.processEvent(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEventId()).isEqualTo("evt-123");

        // CRITICAL IDEMPOTENCY CHECK: Ensure downstream clients and save operations are skipped
        verify(accountClient, never()).applyEvent(any());
        verify(eventRecordRepository, never()).save(any());
    }
}