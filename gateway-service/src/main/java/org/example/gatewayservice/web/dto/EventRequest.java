package org.example.gatewayservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Incoming event request sent to the gateway")
public record EventRequest(
    @NotBlank @Schema(description = "Unique event identifier", example = "evt-12345")
        String eventId,
    @NotBlank @Schema(description = "Account identifier", example = "acc-001") String accountId,
    @NotNull @DecimalMin("0.01") @Schema(description = "Amount to apply", example = "150.75")
        BigDecimal amount,
    @NotBlank
        @Schema(
            description = "Event type",
            example = "CREDIT",
            allowableValues = {"CREDIT", "DEBIT"})
        String type) {}
