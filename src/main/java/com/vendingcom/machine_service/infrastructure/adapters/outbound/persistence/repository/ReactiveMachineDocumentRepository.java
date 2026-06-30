package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.repository;

import com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.entity.MachineDocumentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ReactiveMachineDocumentRepository extends ReactiveCrudRepository<MachineDocumentEntity, Integer> {

    Flux<MachineDocumentEntity> findByMachineId(Integer machineId);
}
