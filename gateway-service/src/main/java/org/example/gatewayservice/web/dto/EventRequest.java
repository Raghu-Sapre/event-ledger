package org.example.gatewayservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Incoming event request sent to the gateway")
public record EventRequest(
    @Schema(description = "Unique event identifier", example = "evt-12345") String eventId,
    @Schema(description = "Account identifier", example = "acc-001") String accountId,
    @Schema(description = "Amount to apply", example = "150.75") BigDecimal amount,
    @Schema(
            description = "Event type",
            example = "CREDIT",
            allowableValues = {"CREDIT", "DEBIT"})
        String type) {}
