package com.vendingcom.machine_service.application.port.output.client;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para validar el cliente dueño de una máquina contra el
 * customer-service. La aplicación NO sabe que por debajo es una llamada HTTP.
 */
public interface CustomerValidationPort {

    /** Devuelve true si el cliente existe en customer-service. */
    Mono<Boolean> customerExists(Integer customerId);
}
