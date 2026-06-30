package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.controller;

import com.vendingcom.machine_service.application.dto.response.MachineParameterResponse;
import com.vendingcom.machine_service.application.port.input.MachineParameterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/machine-parameters")
@Tag(name = "Catálogos", description = "Catálogos del módulo de máquinas (estados, tipos de evento y tipos de documento) para combos del frontend")
@SecurityRequirement(name = "bearerAuth")
public class MachineParameterController {

    private final MachineParameterUseCase machineParameterUseCase;

    public MachineParameterController(MachineParameterUseCase machineParameterUseCase) {
        this.machineParameterUseCase = machineParameterUseCase;
    }

    @Operation(
            summary = "Listar catálogos",
            description = "Lista los parámetros activos. Si se envía 'group' filtra por grupo "
                    + "(ej: MACHINE_STATUS, EVENT_TYPE, DOCUMENT_TYPE)."
    )
    @GetMapping
    public Flux<MachineParameterResponse> find(
            @RequestParam(name = "group", required = false) String group
    ) {
        return (group == null || group.isBlank()
                ? machineParameterUseCase.findAllActive()
                : machineParameterUseCase.findByGroup(group))
                .map(MachineParameterResponse::fromDomain);
    }
}
