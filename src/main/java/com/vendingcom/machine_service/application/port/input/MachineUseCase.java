package com.vendingcom.machine_service.application.port.input;

import com.vendingcom.machine_service.application.dto.request.CreateMachineRequest;
import com.vendingcom.machine_service.application.dto.request.UpdateMachineRequest;
import com.vendingcom.machine_service.application.dto.response.PagedResponse;
import com.vendingcom.machine_service.domain.model.Machine;
import reactor.core.publisher.Mono;

public interface MachineUseCase {

    Mono<Machine> create(CreateMachineRequest request);

    Mono<Machine> update(Integer machineId, UpdateMachineRequest request);

    Mono<Machine> findById(Integer machineId);

    /** Búsqueda paginada con filtros opcionales (texto, cliente, ubicación, estado). */
    Mono<PagedResponse<Machine>> search(String search, Integer customerId, Integer locationId, Integer statusId, int page, int size);

    Mono<Machine> activate(Integer machineId);

    Mono<Void> deactivate(Integer machineId);

    /** Cambia el estado de la máquina a un código válido del grupo MACHINE_STATUS. */
    Mono<Machine> changeStatus(Integer machineId, String statusCode);
}
