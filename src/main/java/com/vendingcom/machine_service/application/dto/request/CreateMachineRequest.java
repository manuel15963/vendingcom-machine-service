package com.vendingcom.machine_service.application.dto.request;

import com.vendingcom.machine_service.util.validation.ValidJson;
import jakarta.validation.constraints.NotNull;
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

        LocalDate installationDate,

        LocalDate lastMaintenanceDate,

        @ValidJson(message = "La configuración debe ser un JSON válido")
        String configuration,

        @Size(max = 2000, message = "Las notas no deben superar 2000 caracteres")
        String notes,

        @Size(max = 255, message = "El código QR no debe superar 255 caracteres")
        String qrCode
) {
}
