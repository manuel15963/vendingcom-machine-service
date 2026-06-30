package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.controller;

import com.vendingcom.machine_service.application.dto.request.CreateMachineDocumentRequest;
import com.vendingcom.machine_service.application.dto.response.MachineDocumentResponse;
import com.vendingcom.machine_service.application.dto.response.MessageResponse;
import com.vendingcom.machine_service.application.port.input.MachineDocumentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/documents")
@Tag(name = "Documentos", description = "Documentos asociados a una máquina")
@SecurityRequirement(name = "bearerAuth")
public class MachineDocumentController {

    private final MachineDocumentUseCase machineDocumentUseCase;

    public MachineDocumentController(MachineDocumentUseCase machineDocumentUseCase) {
        this.machineDocumentUseCase = machineDocumentUseCase;
    }

    @Operation(summary = "Listar documentos de la máquina")
    @GetMapping
    public Flux<MachineDocumentResponse> findByMachine(@PathVariable Integer machineId) {
        return machineDocumentUseCase.findByMachine(machineId).map(MachineDocumentResponse::fromDomain);
    }

    @Operation(summary = "Crear documento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MachineDocumentResponse> create(
            @PathVariable Integer machineId,
            @Valid @RequestBody CreateMachineDocumentRequest request
    ) {
        return machineDocumentUseCase.create(machineId, request).map(MachineDocumentResponse::fromDomain);
    }

    @Operation(summary = "Eliminar documento")
    @DeleteMapping("/{documentId}")
    public Mono<MessageResponse> delete(
            @PathVariable Integer machineId,
            @PathVariable Integer documentId
    ) {
        return machineDocumentUseCase.delete(machineId, documentId)
                .thenReturn(MessageResponse.of("DOCUMENT_DELETED", "Documento eliminado correctamente."));
    }
}
