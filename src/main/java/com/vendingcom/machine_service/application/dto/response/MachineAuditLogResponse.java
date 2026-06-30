package com.vendingcom.machine_service.application.dto.response;

import com.vendingcom.machine_service.domain.model.MachineAuditLog;

import java.time.LocalDateTime;

public record MachineAuditLogResponse(
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
    public static MachineAuditLogResponse fromDomain(MachineAuditLog auditLog) {
        return new MachineAuditLogResponse(
                auditLog.auditLogId(),
                auditLog.machineId(),
                auditLog.affectedTableName(),
                auditLog.affectedRecordId(),
                auditLog.actionType(),
                auditLog.actionDescription(),
                auditLog.oldData(),
                auditLog.newData(),
                auditLog.ipAddress(),
                auditLog.userAgent(),
                auditLog.executedByUserId(),
                auditLog.executedAt()
        );
    }
}
