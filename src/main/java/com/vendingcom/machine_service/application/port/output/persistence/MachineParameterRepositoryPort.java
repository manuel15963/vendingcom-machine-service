package com.vendingcom.machine_service.application.port.output.persistence;

import com.vendingcom.machine_service.domain.model.MachineParameter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Acceso al catálogo de parámetros (estados, tipos de evento y tipos de documento) del módulo.
 */
public interface MachineParameterRepositoryPort {

    /** Resuelve el id de un parámetro activo por su grupo y código (ej: MACHINE_STATUS / ACTIVE). */
    Mono<Integer> findIdByGroupAndCode(String parameterGroup, String parameterCode);

    /** Verifica que un parámetro exista y pertenezca al grupo indicado (ej: que el id sea un EVENT_TYPE). */
    Mono<Boolean> existsByIdAndGroup(Integer parameterId, String parameterGroup);

    /** Devuelve el código de un parámetro por su id (ej: 3 -> "MAINTENANCE"). Vacío si no existe. */
    Mono<String> findCodeById(Integer parameterId);

    /** Lista los parámetros activos de un grupo, ordenados por sort_order. */
    Flux<MachineParameter> findActiveByGroup(String parameterGroup);

    /** Lista todos los parámetros activos del módulo. */
    Flux<MachineParameter> findAllActive();
}
