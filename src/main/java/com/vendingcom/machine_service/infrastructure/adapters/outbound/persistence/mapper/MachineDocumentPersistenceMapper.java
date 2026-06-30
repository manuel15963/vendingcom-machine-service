package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.mapper;

import com.vendingcom.machine_service.domain.model.MachineDocument;
import com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.entity.MachineDocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class MachineDocumentPersistenceMapper {

    public MachineDocument toDomain(MachineDocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MachineDocument(
                entity.getDocumentId(),
                entity.getMachineId(),
                entity.getDocumentTypeId(),
                entity.getFileName(),
                entity.getFileUrl(),
                entity.getFileSize(),
                entity.getMimeType(),
                entity.getUploadedByUserId(),
                entity.getUploadedAt(),
                null    // documentTypeName se resuelve en lecturas (JOIN)
        );
    }

    public MachineDocumentEntity toEntity(MachineDocument domain) {
        if (domain == null) {
            return null;
        }
        return MachineDocumentEntity.builder()
                .documentId(domain.documentId())
                .machineId(domain.machineId())
                .documentTypeId(domain.documentTypeId())
                .fileName(domain.fileName())
                .fileUrl(domain.fileUrl())
                .fileSize(domain.fileSize())
                .mimeType(domain.mimeType())
                .uploadedByUserId(domain.uploadedByUserId())
                .uploadedAt(domain.uploadedAt())
                .build();
    }
}
