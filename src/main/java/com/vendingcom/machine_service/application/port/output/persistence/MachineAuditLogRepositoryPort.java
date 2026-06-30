package com.vendingcom.machine_service.application.port.output.persistence;

import com.vendingcom.machine_service.domain.model.MachineAuditLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface MachineAuditLogRepositoryPort {

    Mono<MachineAuditLog> save(MachineAuditLog auditLog);

    Flux<MachineAuditLog> findAll();

    Flux<MachineAuditLog> findByMachineId(Integer machineId);

    Flux<MachineAuditLog> findByActionType(String actionType);

    /** Elimina los registros de auditoría anteriores a la fecha indicada (retención). */
    Mono<Void> deleteOlderThan(LocalDateTime threshold);
}
