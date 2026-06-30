package com.vendingcom.machine_service.application.port.output.client;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para validar la ubicación donde se instala una máquina contra el
 * location-service. La aplicación NO sabe que por debajo es una llamada HTTP.
 */
public interface LocationValidationPort {

    /** Devuelve true si la ubicación existe en location-service. */
    Mono<Boolean> locationExists(Integer locationId);
}
