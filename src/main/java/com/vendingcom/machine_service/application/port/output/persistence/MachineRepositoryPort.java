package com.vendingcom.machine_service.application.port.output.persistence;

import com.vendingcom.machine_service.domain.model.Machine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface MachineRepositoryPort {

    /** Inserta la máquina (code y qr_code los completa el trigger de la BD). */
    Mono<Machine> create(Machine machine);

    /**
     * Actualiza la máquina con bloqueo optimista: solo escribe si la versión coincide.
     * Si otra operación la modificó, lanza OptimisticLockingFailureException.
     */
    Mono<Machine> update(Machine machine);

    Mono<Machine> findById(Integer machineId);

    /** Búsqueda paginada con filtros opcionales (texto, cliente, ubicación, estado). */
    Flux<Machine> search(String search, Integer customerId, Integer locationId, Integer statusId, int limit, long offset);

    /** Total de resultados para los mismos filtros (para la paginación). */
    Mono<Long> countSearch(String search, Integer customerId, Integer locationId, Integer statusId);

    /** Actualiza SOLO la fecha de último mantenimiento (efecto de registrar un evento de mantenimiento). */
    Mono<Void> updateLastMaintenanceDate(Integer machineId, LocalDate maintenanceDate);
}
