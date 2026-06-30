package com.vendingcom.machine_service.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Máquina vending (activo físico). El cliente (customerId) proviene de customer-service y
 * la ubicación (locationId) de location-service, ambos sin FK física.
 */
public record Machine(
        Integer machineId,
        String code,
        String qrCode,
        Integer customerId,
        Integer locationId,
        String model,
        String brand,
        String serialNumber,
        Integer machineStatusId,
        LocalDate installationDate,
        LocalDate lastMaintenanceDate,
        String configuration,
        String notes,
        Integer version,
        Integer createdByUserId,
        Integer updatedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // Etiqueta legible resuelta desde el catálogo (solo lectura; null en escrituras).
        String machineStatusName
) {
}
