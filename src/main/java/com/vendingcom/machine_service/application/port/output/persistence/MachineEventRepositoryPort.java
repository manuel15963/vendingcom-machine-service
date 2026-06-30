package com.vendingcom.machine_service.application.port.output.persistence;

import com.vendingcom.machine_service.domain.model.MachineEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MachineEventRepositoryPort {

    Mono<MachineEvent> save(MachineEvent event);

    Mono<MachineEvent> findById(Integer eventId);

    /** Eventos de la máquina ordenados del más reciente al más antiguo. */
    Flux<MachineEvent> findByMachineId(Integer machineId);
}
