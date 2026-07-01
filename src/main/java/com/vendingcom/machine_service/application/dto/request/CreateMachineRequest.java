package com.vendingcom.machine_service.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMachineRequest(

        @NotNull(message = "El cliente es obligatorio")
        Integer customerId,

        @NotNull(message = "La ubicación es obligatoria")
        Integer locationId,

        @Size(max = 100, message = "El modelo no debe superar 100 caracteres")
        String model,

        @Size(max = 100, message = "La marca no debe superar 100 caracteres")
        String brand,

        @Size(max = 100, message = "El número de serie no debe superar 100 caracteres")
        String serialNumber,

        @PastOrPresent(message = "La fecha de instalación no puede ser futura")
        LocalDate installationDate,

        @PastOrPresent(message = "El último mantenimiento no puede ser una fecha futura")
        LocalDate lastMaintenanceDate,

        Integer machineTypeId,

        @Min(value = 1, message = "El intervalo de mantenimiento debe ser de al menos 1 día")
        @Max(value = 3650, message = "El intervalo de mantenimiento no debe superar 3650 días")
        Integer maintenanceIntervalDays,

        @Size(max = 2000, message = "Las notas no deben superar 2000 caracteres")
        String notes,

        @Size(max = 255, message = "El código QR no debe superar 255 caracteres")
        String qrCode
) {
}
