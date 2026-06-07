package org.example.gatewayservice.domain;

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
public class EventRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String eventId;
  private String accountId;
  private BigDecimal amount;
  private String type;
  private Instant eventTime;
}
