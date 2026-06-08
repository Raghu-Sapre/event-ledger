package org.example.accountservice.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request to apply an event to an account")
public record ApplyEventRequest(
    @NotBlank String eventId,
    @NotBlank String accountId,
    @NotNull @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,
    @NotBlank String type,
    @NotNull
        @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ssX",
            timezone = "UTC")
        @Schema(description = "The physical timestamp", example = "2026-06-08T05:00:00Z")
        Instant eventTimestamp) {}
