package com.vendingcom.machine_service.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMachineDocumentRequest(

        @NotNull(message = "El tipo de documento es obligatorio")
        Integer documentTypeId,

        @NotBlank(message = "El nombre del archivo es obligatorio")
        @Size(max = 255, message = "El nombre del archivo no debe superar 255 caracteres")
        String fileName,

        @NotBlank(message = "La URL del archivo es obligatoria")
        @Size(max = 500, message = "La URL del archivo no debe superar 500 caracteres")
        String fileUrl,

        Long fileSize,

        @Size(max = 100, message = "El tipo MIME no debe superar 100 caracteres")
        String mimeType
) {
}
