package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        Integer status,
        String code,
        String message
) {
}
