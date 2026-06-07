package org.example.accountservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Request to apply an event to an account")
public record ApplyEventRequest(
    @NotBlank @Schema(description = "Account identifier", example = "acc-001") String accountId,
    @NotNull @DecimalMin(value = "0.01") @Schema(description = "Amount to apply", example = "50.00")
        BigDecimal amount,
    @NotBlank
        @Schema(
            description = "Event type",
            example = "DEBIT",
            allowableValues = {"CREDIT", "DEBIT"})
        String type) {}
