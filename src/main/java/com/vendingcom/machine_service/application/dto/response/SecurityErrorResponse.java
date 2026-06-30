package com.vendingcom.machine_service.application.dto.response;

import java.time.LocalDateTime;

public record SecurityErrorResponse(
        LocalDateTime timestamp,
        Integer status,
        String code,
        String message,
        String path
) {
}
