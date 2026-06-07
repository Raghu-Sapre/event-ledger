package org.example.accountservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Request to apply an event to an account")
public record ApplyEventRequest(
    @Schema(description = "Account identifier", example = "acc-001") String accountId,
    @Schema(description = "Amount to apply", example = "50.00") BigDecimal amount,
    @Schema(
            description = "Event type",
            example = "DEBIT",
            allowableValues = {"CREDIT", "DEBIT"})
        String type) {}
