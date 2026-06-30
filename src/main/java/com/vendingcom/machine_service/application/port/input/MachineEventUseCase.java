package com.vendingcom.machine_service.application.port.input;

import com.vendingcom.machine_service.application.dto.request.CreateMachineEventRequest;
import com.vendingcom.machine_service.domain.model.MachineEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MachineEventUseCase {

    Mono<MachineEvent> create(Integer machineId, CreateMachineEventRequest request);

    Flux<MachineEvent> findByMachine(Integer machineId);
}
