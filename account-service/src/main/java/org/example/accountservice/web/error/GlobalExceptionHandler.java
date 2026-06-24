package org.example.accountservice.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j; // Added for structured error tracing
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j // Injects Slf4j logger instance
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();

    // Log the validation error so it captures the trace ID automatically
    log.warn("Payload validation failed for route [{}]: {}", request.getRequestURI(), details);

    ErrorResponse body =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Validation failed",
            request.getRequestURI(),
            Instant.now(),
            details);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn(
        "Invalid business argument provided at [{}]: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse body =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            Instant.now(),
            List.of());
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(org.example.accountservice.exception.AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
    log.warn(
        "Resource context missing lookup at [{}]: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse body =
        new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            Instant.now(),
            List.of());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    // CRITICAL: Log unexpected 500 crashes with full stack traces tied to your request Trace ID
    log.error("Unhandled runtime exception encountered at route [{}]", request.getRequestURI(), ex);

    ErrorResponse body =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            ex.getMessage(),
            request.getRequestURI(),
            Instant.now(),
            List.of());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
