package com.vendingcom.machine_service.application.port.input;

import com.vendingcom.machine_service.domain.model.MachineAuditLog;
import reactor.core.publisher.Flux;

public interface MachineAuditLogUseCase {

    Flux<MachineAuditLog> findAll();

    Flux<MachineAuditLog> findByMachine(Integer machineId);

    Flux<MachineAuditLog> findByActionType(String actionType);
}
