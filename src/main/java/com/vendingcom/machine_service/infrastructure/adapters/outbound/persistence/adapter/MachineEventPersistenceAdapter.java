package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.adapter;

import com.vendingcom.machine_service.application.port.output.persistence.MachineEventRepositoryPort;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import com.vendingcom.machine_service.domain.model.MachineEvent;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class MachineEventPersistenceAdapter implements MachineEventRepositoryPort {

    // Lectura con JOIN al catálogo para traer la etiqueta legible del tipo de evento.
    private static final String SELECT_WITH_LABELS = """
            SELECT e.event_id, e.machine_id, e.event_type_id, e.title, e.description,
                   e.performed_by_user_id, e.event_date, e.metadata::text AS metadata, e.created_at,
                   t.parameter_value AS event_type_name
            FROM machine_events e
            LEFT JOIN machine_parameters t ON t.parameter_id = e.event_type_id
            """;

    private final DatabaseClient databaseClient;

    public MachineEventPersistenceAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<MachineEvent> save(MachineEvent event) {
        String sql = """
                INSERT INTO machine_events (
                    machine_id, event_type_id, title, description,
                    performed_by_user_id, event_date, metadata
                ) VALUES (
                    :machineId, :eventTypeId, :title, :description,
                    :performedByUserId, :eventDate, CAST(:metadata AS jsonb)
                )
                RETURNING event_id
                """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        spec = spec.bind("machineId", event.machineId());
        spec = spec.bind("eventTypeId", event.eventTypeId());
        spec = spec.bind("title", event.title());
        spec = bindNullable(spec, "description", event.description(), String.class);
        spec = bindNullable(spec, "performedByUserId", event.performedByUserId(), Integer.class);
        spec = spec.bind("eventDate", event.eventDate() == null ? LocalDateTime.now() : event.eventDate());
        spec = bindNullable(spec, "metadata", event.metadata(), String.class);

        return spec.map((row, metadata) -> row.get("event_id", Integer.class))
                .one()
                .flatMap(this::findById)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "El evento recién creado no pudo recuperarse tras la inserción.")));
    }

    @Override
    public Mono<MachineEvent> findById(Integer eventId) {
        return databaseClient.sql(SELECT_WITH_LABELS + " WHERE e.event_id = :id")
                .bind("id", eventId)
                .map((row, metadata) -> mapRow(row))
                .one();
    }

    @Override
    public Flux<MachineEvent> findByMachineId(Integer machineId) {
        return databaseClient.sql(SELECT_WITH_LABELS + " WHERE e.machine_id = :machineId ORDER BY e.event_date DESC")
                .bind("machineId", machineId)
                .map((row, metadata) -> mapRow(row))
                .all();
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private MachineEvent mapRow(Row row) {
        return new MachineEvent(
                row.get("event_id", Integer.class),
                row.get("machine_id", Integer.class),
                row.get("event_type_id", Integer.class),
                row.get("title", String.class),
                row.get("description", String.class),
                row.get("performed_by_user_id", Integer.class),
                row.get("event_date", LocalDateTime.class),
                row.get("metadata", String.class),
                row.get("created_at", LocalDateTime.class),
                row.get("event_type_name", String.class)
        );
    }
}
