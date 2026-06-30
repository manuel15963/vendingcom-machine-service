package com.vendingcom.machine_service.domain.model;

import java.time.LocalDateTime;

/**
 * Evento ocurrido en una máquina (instalación, mantenimiento, reparación, etc.).
 */
public record MachineEvent(
        Integer eventId,
        Integer machineId,
        Integer eventTypeId,
        String title,
        String description,
        Integer performedByUserId,
        LocalDateTime eventDate,
        String metadata,
        LocalDateTime createdAt,
        // Etiqueta legible resuelta desde el catálogo (solo lectura).
        String eventTypeName
) {
}
