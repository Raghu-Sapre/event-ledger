package org.example.gatewayservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Event record stored by the gateway service")
public class EventRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "Database identifier", example = "1")
  private Long id;

  @Schema(description = "Unique event identifier", example = "evt-12345")
  private String eventId;

  @Schema(description = "Account identifier", example = "acc-001")
  private String accountId;

  @Schema(description = "Amount applied", example = "150.75")
  private BigDecimal amount;

  @Schema(description = "Event type", example = "CREDIT")
  private String type;

  @Schema(description = "Timestamp when the event was processed")
  private Instant eventTime;
}
