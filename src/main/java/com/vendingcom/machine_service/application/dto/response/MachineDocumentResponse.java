package com.vendingcom.machine_service.application.dto.response;

import com.vendingcom.machine_service.domain.model.MachineDocument;

import java.time.LocalDateTime;

public record MachineDocumentResponse(
        Integer documentId,
        Integer machineId,
        Integer documentTypeId,
        String documentTypeName,
        String fileName,
        String fileUrl,
        Long fileSize,
        String mimeType,
        Integer uploadedByUserId,
        LocalDateTime uploadedAt
) {
    public static MachineDocumentResponse fromDomain(MachineDocument document) {
        return new MachineDocumentResponse(
                document.documentId(),
                document.machineId(),
                document.documentTypeId(),
                document.documentTypeName(),
                document.fileName(),
                document.fileUrl(),
                document.fileSize(),
                document.mimeType(),
                document.uploadedByUserId(),
                document.uploadedAt()
        );
    }
}
