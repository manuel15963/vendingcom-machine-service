package com.vendingcom.machine_service.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambia el estado de la máquina a un código del grupo MACHINE_STATUS
 * (ej: ACTIVE, INACTIVE, MAINTENANCE, OUT_OF_SERVICE).
 */
public record ChangeMachineStatusRequest(

        @NotBlank(message = "El código de estado es obligatorio")
        @Size(max = 50, message = "El código de estado no debe superar 50 caracteres")
        String code
) {
}
