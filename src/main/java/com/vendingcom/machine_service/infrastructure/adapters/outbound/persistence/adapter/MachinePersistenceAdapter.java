package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.adapter;

import com.vendingcom.machine_service.application.port.output.persistence.MachineRepositoryPort;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import com.vendingcom.machine_service.domain.model.Machine;
import io.r2dbc.spi.Row;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class MachinePersistenceAdapter implements MachineRepositoryPort {

    // Lectura con JOIN al catálogo para traer la etiqueta legible del estado (parameter_value).
    private static final String SELECT_WITH_LABELS = """
            SELECT m.machine_id, m.code, m.qr_code, m.customer_id, m.location_id, m.model, m.brand,
                   m.serial_number, m.machine_status_id, m.machine_type_id,
                   m.installation_date, m.last_maintenance_date, m.maintenance_interval_days,
                   m.notes, m.version,
                   m.created_by_user_id, m.updated_by_user_id, m.created_at, m.updated_at,
                   s.parameter_value AS machine_status_name,
                   t.parameter_value AS machine_type_name
            FROM machines m
            LEFT JOIN machine_parameters s ON s.parameter_id = m.machine_status_id
            LEFT JOIN machine_parameters t ON t.parameter_id = m.machine_type_id
            """;

    private final DatabaseClient databaseClient;

    public MachinePersistenceAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Machine> create(Machine machine) {
        // code y qr_code se pasan NULL: el trigger de la BD los completa (VEND-000001).
        String sql = """
                INSERT INTO machines (
                    code, qr_code, customer_id, location_id, model, brand, serial_number,
                    machine_status_id, machine_type_id, installation_date, last_maintenance_date,
                    maintenance_interval_days, notes, version, created_by_user_id, updated_by_user_id
                ) VALUES (
                    :code, :qrCode, :customerId, :locationId, :model, :brand, :serialNumber,
                    :machineStatusId, :machineTypeId, :installationDate, :lastMaintenanceDate,
                    :maintenanceIntervalDays, :notes, 0, :createdByUserId, :updatedByUserId
                )
                RETURNING machine_id
                """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        spec = bindNullable(spec, "code", machine.code(), String.class);
        spec = bindNullable(spec, "qrCode", machine.qrCode(), String.class);
        spec = spec.bind("customerId", machine.customerId());
        spec = spec.bind("locationId", machine.locationId());
        spec = bindNullable(spec, "model", machine.model(), String.class);
        spec = bindNullable(spec, "brand", machine.brand(), String.class);
        spec = bindNullable(spec, "serialNumber", machine.serialNumber(), String.class);
        spec = spec.bind("machineStatusId", machine.machineStatusId());
        spec = bindNullable(spec, "machineTypeId", machine.machineTypeId(), Integer.class);
        spec = bindNullable(spec, "installationDate", machine.installationDate(), LocalDate.class);
        spec = bindNullable(spec, "lastMaintenanceDate", machine.lastMaintenanceDate(), LocalDate.class);
        spec = bindNullable(spec, "maintenanceIntervalDays", machine.maintenanceIntervalDays(), Integer.class);
        spec = bindNullable(spec, "notes", machine.notes(), String.class);
        spec = bindNullable(spec, "createdByUserId", machine.createdByUserId(), Integer.class);
        spec = bindNullable(spec, "updatedByUserId", machine.updatedByUserId(), Integer.class);

        // Tras insertar, re-leemos la fila para devolver el code/qr generados por el trigger
        // y la etiqueta del estado resuelta por el JOIN.
        return spec.map((row, metadata) -> row.get("machine_id", Integer.class))
                .one()
                .flatMap(this::findById)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "La máquina recién creada no pudo recuperarse tras la inserción.")));
    }

    @Override
    public Mono<Machine> update(Machine machine) {
        // Bloqueo optimista: solo actualiza si la versión coincide; si no, 0 filas -> error.
        String sql = """
                UPDATE machines SET
                    model = :model,
                    brand = :brand,
                    serial_number = :serialNumber,
                    machine_status_id = :machineStatusId,
                    machine_type_id = :machineTypeId,
                    installation_date = :installationDate,
                    last_maintenance_date = :lastMaintenanceDate,
                    maintenance_interval_days = :maintenanceIntervalDays,
                    notes = :notes,
                    version = version + 1,
                    updated_by_user_id = :updatedByUserId
                WHERE machine_id = :machineId AND version = :version
                """;
        // NOTA: no se liga updated_at; el trigger BEFORE UPDATE trg_machines_updated_at
        // lo fija siempre a CURRENT_TIMESTAMP. Ligarlo aquí era código muerto.

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        spec = bindNullable(spec, "model", machine.model(), String.class);
        spec = bindNullable(spec, "brand", machine.brand(), String.class);
        spec = bindNullable(spec, "serialNumber", machine.serialNumber(), String.class);
        spec = spec.bind("machineStatusId", machine.machineStatusId());
        spec = bindNullable(spec, "machineTypeId", machine.machineTypeId(), Integer.class);
        spec = bindNullable(spec, "installationDate", machine.installationDate(), LocalDate.class);
        spec = bindNullable(spec, "lastMaintenanceDate", machine.lastMaintenanceDate(), LocalDate.class);
        spec = bindNullable(spec, "maintenanceIntervalDays", machine.maintenanceIntervalDays(), Integer.class);
        spec = bindNullable(spec, "notes", machine.notes(), String.class);
        spec = bindNullable(spec, "updatedByUserId", machine.updatedByUserId(), Integer.class);
        spec = spec.bind("machineId", machine.machineId());
        spec = spec.bind("version", machine.version());

        return spec.fetch().rowsUpdated()
                .flatMap(rows -> rows == 0
                        ? Mono.error(new OptimisticLockingFailureException(
                        "La máquina " + machine.machineId() + " fue modificada por otra operación."))
                        : findById(machine.machineId())
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "La máquina " + machine.machineId() + " no pudo recuperarse tras la actualización."))));
    }

    @Override
    public Mono<Machine> findById(Integer machineId) {
        return databaseClient.sql(SELECT_WITH_LABELS + " WHERE m.machine_id = :id")
                .bind("id", machineId)
                .map((row, metadata) -> mapRow(row))
                .one();
    }

    @Override
    public Flux<Machine> search(String search, Integer customerId, Integer locationId, Integer statusId, int limit, long offset) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_LABELS + " WHERE 1 = 1");
        appendFilters(sql, search, customerId, locationId, statusId);
        sql.append(" ORDER BY m.machine_id DESC LIMIT :limit OFFSET :offset");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        spec = bindFilters(spec, search, customerId, locationId, statusId);
        spec = spec.bind("limit", limit).bind("offset", offset);

        return spec.map((row, metadata) -> mapRow(row)).all();
    }

    @Override
    public Mono<Long> countSearch(String search, Integer customerId, Integer locationId, Integer statusId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM machines m WHERE 1 = 1");
        appendFilters(sql, search, customerId, locationId, statusId);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        spec = bindFilters(spec, search, customerId, locationId, statusId);

        return spec.map((row, metadata) -> row.get("total", Long.class)).one();
    }

    @Override
    public Mono<Void> updateLastMaintenanceDate(Integer machineId, LocalDate maintenanceDate) {
        // version + 1: si una edición concurrente quedó con versión obsoleta, fallará (optimistic lock)
        // en vez de pisar silenciosamente esta fecha de mantenimiento.
        return databaseClient.sql(
                        "UPDATE machines SET last_maintenance_date = :date, version = version + 1 WHERE machine_id = :id")
                .bind("date", maintenanceDate)
                .bind("id", machineId)
                .fetch().rowsUpdated()
                .then();
    }

    private void appendFilters(StringBuilder sql, String search, Integer customerId, Integer locationId, Integer statusId) {
        if (search != null) {
            sql.append(" AND (LOWER(COALESCE(m.code, '')) LIKE :search"
                    + " OR LOWER(COALESCE(m.serial_number, '')) LIKE :search"
                    + " OR LOWER(COALESCE(m.model, '')) LIKE :search)");
        }
        if (customerId != null) {
            sql.append(" AND m.customer_id = :customerId");
        }
        if (locationId != null) {
            sql.append(" AND m.location_id = :locationId");
        }
        if (statusId != null) {
            sql.append(" AND m.machine_status_id = :statusId");
        }
    }

    private DatabaseClient.GenericExecuteSpec bindFilters(
            DatabaseClient.GenericExecuteSpec spec, String search, Integer customerId, Integer locationId, Integer statusId) {
        if (search != null) {
            spec = spec.bind("search", "%" + search.toLowerCase() + "%");
        }
        if (customerId != null) {
            spec = spec.bind("customerId", customerId);
        }
        if (locationId != null) {
            spec = spec.bind("locationId", locationId);
        }
        if (statusId != null) {
            spec = spec.bind("statusId", statusId);
        }
        return spec;
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private Machine mapRow(Row row) {
        return new Machine(
                row.get("machine_id", Integer.class),
                row.get("code", String.class),
                row.get("qr_code", String.class),
                row.get("customer_id", Integer.class),
                row.get("location_id", Integer.class),
                row.get("model", String.class),
                row.get("brand", String.class),
                row.get("serial_number", String.class),
                row.get("machine_status_id", Integer.class),
                row.get("machine_type_id", Integer.class),
                row.get("installation_date", LocalDate.class),
                row.get("last_maintenance_date", LocalDate.class),
                row.get("maintenance_interval_days", Integer.class),
                row.get("notes", String.class),
                row.get("version", Integer.class),
                row.get("created_by_user_id", Integer.class),
                row.get("updated_by_user_id", Integer.class),
                row.get("created_at", LocalDateTime.class),
                row.get("updated_at", LocalDateTime.class),
                row.get("machine_status_name", String.class),
                row.get("machine_type_name", String.class)
        );
    }
}
