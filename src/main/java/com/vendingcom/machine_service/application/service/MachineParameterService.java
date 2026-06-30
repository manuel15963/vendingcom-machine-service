package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.port.input.MachineParameterUseCase;
import com.vendingcom.machine_service.application.port.output.persistence.MachineParameterRepositoryPort;
import com.vendingcom.machine_service.domain.model.MachineParameter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MachineParameterService implements MachineParameterUseCase {

    private final MachineParameterRepositoryPort machineParameterRepositoryPort;

    public MachineParameterService(MachineParameterRepositoryPort machineParameterRepositoryPort) {
        this.machineParameterRepositoryPort = machineParameterRepositoryPort;
    }

    @Override
    public Flux<MachineParameter> findByGroup(String parameterGroup) {
        String normalizedGroup = parameterGroup == null ? null : parameterGroup.trim().toUpperCase();
        return machineParameterRepositoryPort.findActiveByGroup(normalizedGroup);
    }

    @Override
    public Flux<MachineParameter> findAllActive() {
        return machineParameterRepositoryPort.findAllActive();
    }
}
