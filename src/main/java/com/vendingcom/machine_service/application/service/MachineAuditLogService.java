package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.port.input.MachineAuditLogUseCase;
import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import com.vendingcom.machine_service.domain.model.MachineAuditLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MachineAuditLogService implements MachineAuditLogUseCase {

    private final MachineAuditLogRepositoryPort auditLogRepositoryPort;

    public MachineAuditLogService(MachineAuditLogRepositoryPort auditLogRepositoryPort) {
        this.auditLogRepositoryPort = auditLogRepositoryPort;
    }

    @Override
    public Flux<MachineAuditLog> findAll() {
        return auditLogRepositoryPort.findAll();
    }

    @Override
    public Flux<MachineAuditLog> findByMachine(Integer machineId) {
        return auditLogRepositoryPort.findByMachineId(machineId);
    }

    @Override
    public Flux<MachineAuditLog> findByActionType(String actionType) {
        String normalized = actionType == null ? null : actionType.trim().toUpperCase();
        return auditLogRepositoryPort.findByActionType(normalized);
    }
}
