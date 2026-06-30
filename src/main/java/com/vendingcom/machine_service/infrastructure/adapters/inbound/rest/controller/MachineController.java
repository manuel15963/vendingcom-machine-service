package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.controller;

import com.vendingcom.machine_service.application.dto.request.ChangeMachineStatusRequest;
import com.vendingcom.machine_service.application.dto.request.CreateMachineRequest;
import com.vendingcom.machine_service.application.dto.request.UpdateMachineRequest;
import com.vendingcom.machine_service.application.dto.response.MachineResponse;
import com.vendingcom.machine_service.application.dto.response.MessageResponse;
import com.vendingcom.machine_service.application.dto.response.PagedResponse;
import com.vendingcom.machine_service.application.port.input.MachineUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/machines")
@Tag(name = "Máquinas", description = "Gestión de máquinas expendedoras")
@SecurityRequirement(name = "bearerAuth")
public class MachineController {

    private final MachineUseCase machineUseCase;

    public MachineController(MachineUseCase machineUseCase) {
        this.machineUseCase = machineUseCase;
    }

    @Operation(
            summary = "Buscar / listar máquinas (paginado)",
            description = "Filtros opcionales: 'search' (texto en código/modelo/marca/serie), 'customerId', 'locationId', 'statusId'. "
                    + "Paginación con 'page' (desde 0) y 'size' (máx. 100)."
    )
    @GetMapping
    public Mono<PagedResponse<MachineResponse>> search(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "customerId", required = false) Integer customerId,
            @RequestParam(name = "locationId", required = false) Integer locationId,
            @RequestParam(name = "statusId", required = false) Integer statusId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return machineUseCase.search(search, customerId, locationId, statusId, page, size)
                .map(paged -> paged.map(MachineResponse::fromDomain));
    }

    @Operation(summary = "Buscar máquina por ID")
    @GetMapping("/{machineId}")
    public Mono<MachineResponse> findById(@PathVariable Integer machineId) {
        return machineUseCase.findById(machineId).map(MachineResponse::fromDomain);
    }

    @Operation(summary = "Crear máquina")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MachineResponse> create(@Valid @RequestBody CreateMachineRequest request) {
        return machineUseCase.create(request).map(MachineResponse::fromDomain);
    }

    @Operation(summary = "Actualizar máquina")
    @PutMapping("/{machineId}")
    public Mono<MachineResponse> update(
            @PathVariable Integer machineId,
            @Valid @RequestBody UpdateMachineRequest request
    ) {
        return machineUseCase.update(machineId, request).map(MachineResponse::fromDomain);
    }

    @Operation(summary = "Activar máquina")
    @PatchMapping("/{machineId}/activate")
    public Mono<MachineResponse> activate(@PathVariable Integer machineId) {
        return machineUseCase.activate(machineId).map(MachineResponse::fromDomain);
    }

    @Operation(summary = "Desactivar máquina (eliminación lógica)")
    @DeleteMapping("/{machineId}")
    public Mono<MessageResponse> deactivate(@PathVariable Integer machineId) {
        return machineUseCase.deactivate(machineId)
                .thenReturn(MessageResponse.of("MACHINE_DEACTIVATED", "Máquina desactivada correctamente."));
    }

    @Operation(
            summary = "Cambiar estado de la máquina",
            description = "Cambia el estado a un código válido del grupo MACHINE_STATUS "
                    + "(ej: ACTIVE, INACTIVE, MAINTENANCE, OUT_OF_SERVICE)."
    )
    @PatchMapping("/{machineId}/status")
    public Mono<MachineResponse> changeStatus(
            @PathVariable Integer machineId,
            @Valid @RequestBody ChangeMachineStatusRequest request
    ) {
        return machineUseCase.changeStatus(machineId, request.code()).map(MachineResponse::fromDomain);
    }
}
