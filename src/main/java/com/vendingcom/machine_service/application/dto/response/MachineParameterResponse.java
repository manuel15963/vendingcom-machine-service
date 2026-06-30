package com.vendingcom.machine_service.application.dto.response;

import com.vendingcom.machine_service.domain.model.MachineParameter;

public record MachineParameterResponse(
        Integer parameterId,
        String parameterGroup,
        String parameterCode,
        String parameterValue,
        String description,
        Integer sortOrder
) {
    public static MachineParameterResponse fromDomain(MachineParameter parameter) {
        return new MachineParameterResponse(
                parameter.parameterId(),
                parameter.parameterGroup(),
                parameter.parameterCode(),
                parameter.parameterValue(),
                parameter.description(),
                parameter.sortOrder()
        );
    }
}
