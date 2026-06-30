package com.vendingcom.machine_service.application.port.input;

import com.vendingcom.machine_service.application.dto.request.CreateMachineDocumentRequest;
import com.vendingcom.machine_service.domain.model.MachineDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MachineDocumentUseCase {

    Mono<MachineDocument> create(Integer machineId, CreateMachineDocumentRequest request);

    Flux<MachineDocument> findByMachine(Integer machineId);

    Mono<Void> delete(Integer machineId, Integer documentId);
}
