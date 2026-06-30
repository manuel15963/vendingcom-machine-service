package com.vendingcom.machine_service.application.dto.request;

import com.vendingcom.machine_service.util.validation.ValidJson;
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

        @ValidJson(message = "La configuración debe ser un JSON válido")
        String configuration,

        @Size(max = 2000, message = "Las notas no deben superar 2000 caracteres")
        String notes
) {
}
