package com.vendingcom.machine_service.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Actualiza los datos de una máquina. No cambia el cliente, la ubicación, el código ni el estado
 * (el estado se maneja con activar/desactivar/cambiar estado).
 */
public record UpdateMachineRequest(

        @Size(max = 100, message = "El modelo no debe superar 100 caracteres")
        String model,

        @Size(max = 100, message = "La marca no debe superar 100 caracteres")
        String brand,

        @Size(max = 100, message = "El número de serie no debe superar 100 caracteres")
        String serialNumber,

        LocalDate installationDate,

        LocalDate lastMaintenanceDate,

        Integer machineTypeId,

        @Min(value = 1, message = "El intervalo de mantenimiento debe ser de al menos 1 día")
        @Max(value = 3650, message = "El intervalo de mantenimiento no debe superar 3650 días")
        Integer maintenanceIntervalDays,

        @Size(max = 2000, message = "Las notas no deben superar 2000 caracteres")
        String notes
) {
}
