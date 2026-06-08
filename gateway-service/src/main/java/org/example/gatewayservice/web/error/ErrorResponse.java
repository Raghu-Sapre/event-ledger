package org.example.gatewayservice.web.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error response")
public record ErrorResponse(
    @Schema(example = "400") int status,
    @Schema(example = "Bad Request") String error,
    @Schema(example = "Validation failed") String message,
    @Schema(example = "/accounts/apply-event") String path,
    @Schema(description = "Timestamp of the error") Instant timestamp,
    @Schema(description = "Field-level validation errors") List<String> details) {}
