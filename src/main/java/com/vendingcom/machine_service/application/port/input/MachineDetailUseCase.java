package com.vendingcom.machine_service.application.port.input;

import com.vendingcom.machine_service.application.dto.response.MachineDetailResponse;
import reactor.core.publisher.Mono;

public interface MachineDetailUseCase {

    /** Devuelve la máquina junto con sus eventos y documentos. */
    Mono<MachineDetailResponse> findDetail(Integer machineId);
}
