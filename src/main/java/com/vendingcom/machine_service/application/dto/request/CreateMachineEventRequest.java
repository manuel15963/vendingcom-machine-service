package com.vendingcom.machine_service.application.dto.request;

import com.vendingcom.machine_service.util.validation.ValidJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMachineEventRequest(

        @NotNull(message = "El tipo de evento es obligatorio")
        Integer eventTypeId,

        @NotBlank(message = "El título del evento es obligatorio")
        @Size(max = 150, message = "El título no debe superar 150 caracteres")
        String title,

        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres")
        String description,

        @ValidJson(message = "La metadata debe ser un JSON válido")
        String metadata
) {
}
