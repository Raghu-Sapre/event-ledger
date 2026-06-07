package org.example.gatewayservice.web.dto;

import java.math.BigDecimal;

public record EventRequest(String eventId, String accountId, BigDecimal amount, String type) {}
