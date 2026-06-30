package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.controller;

import com.vendingcom.machine_service.application.dto.response.MachineDetailResponse;
import com.vendingcom.machine_service.application.port.input.MachineDetailUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/detail")
@Tag(name = "Máquinas", description = "Ficha completa de la máquina")
@SecurityRequirement(name = "bearerAuth")
public class MachineDetailController {

    private final MachineDetailUseCase machineDetailUseCase;

    public MachineDetailController(MachineDetailUseCase machineDetailUseCase) {
        this.machineDetailUseCase = machineDetailUseCase;
    }

    @Operation(
            summary = "Ficha completa de la máquina",
            description = "Devuelve la máquina junto con sus eventos y documentos en una sola respuesta."
    )
    @GetMapping
    public Mono<MachineDetailResponse> findDetail(@PathVariable Integer machineId) {
        return machineDetailUseCase.findDetail(machineId);
    }
}
