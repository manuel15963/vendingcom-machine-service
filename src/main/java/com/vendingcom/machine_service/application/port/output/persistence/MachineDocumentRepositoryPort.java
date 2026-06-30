package com.vendingcom.machine_service.application.port.output.persistence;

import com.vendingcom.machine_service.domain.model.MachineDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MachineDocumentRepositoryPort {

    Mono<MachineDocument> save(MachineDocument document);

    Mono<MachineDocument> findById(Integer documentId);

    Flux<MachineDocument> findByMachineId(Integer machineId);

    Mono<Void> deleteById(Integer documentId);
}
