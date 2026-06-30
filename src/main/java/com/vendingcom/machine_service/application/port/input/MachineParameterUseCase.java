package com.vendingcom.machine_service.application.port.input;

import com.vendingcom.machine_service.domain.model.MachineParameter;
import reactor.core.publisher.Flux;

public interface MachineParameterUseCase {

    /** Lista los parámetros activos de un grupo (ej: MACHINE_STATUS) para combos del frontend. */
    Flux<MachineParameter> findByGroup(String parameterGroup);

    /** Lista todos los parámetros activos del módulo. */
    Flux<MachineParameter> findAllActive();
}
