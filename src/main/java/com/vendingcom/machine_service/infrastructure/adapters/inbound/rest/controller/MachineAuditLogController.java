package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.controller;

import com.vendingcom.machine_service.application.dto.response.MachineAuditLogResponse;
import com.vendingcom.machine_service.application.port.input.MachineAuditLogUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/machine-audit-logs")
@Tag(name = "Auditoría", description = "Consulta del historial de cambios del módulo de máquinas (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class MachineAuditLogController {

    private final MachineAuditLogUseCase machineAuditLogUseCase;

    public MachineAuditLogController(MachineAuditLogUseCase machineAuditLogUseCase) {
        this.machineAuditLogUseCase = machineAuditLogUseCase;
    }

    @Operation(summary = "Listar auditoría", description = "Últimos 500 eventos de auditoría, del más reciente al más antiguo.")
    @GetMapping
    public Flux<MachineAuditLogResponse> findAll() {
        return machineAuditLogUseCase.findAll().map(MachineAuditLogResponse::fromDomain);
    }

    @Operation(summary = "Auditoría por máquina")
    @GetMapping("/machine/{machineId}")
    public Flux<MachineAuditLogResponse> findByMachine(@PathVariable Integer machineId) {
        return machineAuditLogUseCase.findByMachine(machineId).map(MachineAuditLogResponse::fromDomain);
    }

    @Operation(summary = "Auditoría por tipo de acción", description = "Ej: MACHINE_CREATED, MACHINE_UPDATED, MACHINE_EVENT_DELETED.")
    @GetMapping("/action/{actionType}")
    public Flux<MachineAuditLogResponse> findByActionType(@PathVariable String actionType) {
        return machineAuditLogUseCase.findByActionType(actionType).map(MachineAuditLogResponse::fromDomain);
    }
}
