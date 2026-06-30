package com.vendingcom.machine_service.application.dto.response;

public record MessageResponse(
        String code,
        String message
) {
    public static MessageResponse of(String code, String message) {
        return new MessageResponse(code, message);
    }
}
