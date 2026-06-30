package com.vendingcom.machine_service.domain.model;

import java.time.LocalDateTime;

/**
 * Registro de auditoría del módulo de máquinas (append-only).
 */
public record MachineAuditLog(
        Long auditLogId,
        Integer machineId,
        String affectedTableName,
        Integer affectedRecordId,
        String actionType,
        String actionDescription,
        String oldData,
        String newData,
        String ipAddress,
        String userAgent,
        Integer executedByUserId,
        LocalDateTime executedAt
) {
}
