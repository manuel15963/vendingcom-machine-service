package com.vendingcom.machine_service.domain.model;

import java.time.LocalDateTime;

/**
 * Documento asociado a una máquina (manual, garantía, ficha técnica, foto, etc.).
 * El archivo vive en S3/MinIO; aquí solo se guarda la URL.
 */
public record MachineDocument(
        Integer documentId,
        Integer machineId,
        Integer documentTypeId,
        String fileName,
        String fileUrl,
        Long fileSize,
        String mimeType,
        Integer uploadedByUserId,
        LocalDateTime uploadedAt,
        // Etiqueta legible resuelta desde el catálogo (solo lectura).
        String documentTypeName
) {
}
