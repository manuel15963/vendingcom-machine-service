package com.vendingcom.machine_service.domain.model;

/**
 * Parámetro de catálogo del módulo (estados, tipos de evento y tipos de documento de la máquina).
 */
public record MachineParameter(
        Integer parameterId,
        String parameterGroup,
        String parameterCode,
        String parameterValue,
        String description,
        Integer sortOrder,
        Integer parameterStatus
) {
}
