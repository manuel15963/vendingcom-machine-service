package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.controller;

import com.vendingcom.machine_service.application.dto.request.CreateMachineEventRequest;
import com.vendingcom.machine_service.application.dto.response.MachineEventResponse;
import com.vendingcom.machine_service.application.port.input.MachineEventUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/events")
@Tag(name = "Eventos", description = "Eventos asociados a una máquina")
@SecurityRequirement(name = "bearerAuth")
public class MachineEventController {

    private final MachineEventUseCase machineEventUseCase;

    public MachineEventController(MachineEventUseCase machineEventUseCase) {
        this.machineEventUseCase = machineEventUseCase;
    }

    @Operation(summary = "Listar eventos de la máquina")
    @GetMapping
    public Flux<MachineEventResponse> findByMachine(@PathVariable Integer machineId) {
        return machineEventUseCase.findByMachine(machineId).map(MachineEventResponse::fromDomain);
    }

    @Operation(summary = "Crear evento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MachineEventResponse> create(
            @PathVariable Integer machineId,
            @Valid @RequestBody CreateMachineEventRequest request
    ) {
        return machineEventUseCase.create(machineId, request).map(MachineEventResponse::fromDomain);
    }
}
