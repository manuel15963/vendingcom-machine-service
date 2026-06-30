package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.adapter;

import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import com.vendingcom.machine_service.domain.model.MachineAuditLog;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class MachineAuditLogPersistenceAdapter implements MachineAuditLogRepositoryPort {

    private final DatabaseClient databaseClient;

    public MachineAuditLogPersistenceAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<MachineAuditLog> save(MachineAuditLog auditLog) {
        String sql = """
                INSERT INTO machine_audit_logs (
                    machine_id,
                    affected_table_name,
                    affected_record_id,
                    action_type,
                    action_description,
                    old_data,
                    new_data,
                    ip_address,
                    user_agent,
                    executed_by_user_id,
                    executed_at
                ) VALUES (
                    :machineId,
                    :affectedTableName,
                    :affectedRecordId,
                    :actionType,
                    :actionDescription,
                    CAST(:oldData AS jsonb),
                    CAST(:newData AS jsonb),
                    :ipAddress,
                    :userAgent,
                    :executedByUserId,
                    :executedAt
                )
                RETURNING
                    audit_log_id,
                    machine_id,
                    affected_table_name,
                    affected_record_id,
                    action_type,
                    action_description,
                    old_data::text AS old_data,
                    new_data::text AS new_data,
                    ip_address,
                    user_agent,
                    executed_by_user_id,
                    executed_at
                """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        spec = auditLog.machineId() == null
                ? spec.bindNull("machineId", Integer.class)
                : spec.bind("machineId", auditLog.machineId());

        spec = auditLog.affectedTableName() == null
                ? spec.bindNull("affectedTableName", String.class)
                : spec.bind("affectedTableName", auditLog.affectedTableName());

        spec = auditLog.affectedRecordId() == null
                ? spec.bindNull("affectedRecordId", Integer.class)
                : spec.bind("affectedRecordId", auditLog.affectedRecordId());

        spec = spec.bind("actionType", auditLog.actionType());

        spec = auditLog.actionDescription() == null
                ? spec.bindNull("actionDescription", String.class)
                : spec.bind("actionDescription", auditLog.actionDescription());

        spec = auditLog.oldData() == null
                ? spec.bindNull("oldData", String.class)
                : spec.bind("oldData", auditLog.oldData());

        spec = auditLog.newData() == null
                ? spec.bindNull("newData", String.class)
                : spec.bind("newData", auditLog.newData());

        spec = auditLog.ipAddress() == null
                ? spec.bindNull("ipAddress", String.class)
                : spec.bind("ipAddress", auditLog.ipAddress());

        spec = auditLog.userAgent() == null
                ? spec.bindNull("userAgent", String.class)
                : spec.bind("userAgent", auditLog.userAgent());

        spec = auditLog.executedByUserId() == null
                ? spec.bindNull("executedByUserId", Integer.class)
                : spec.bind("executedByUserId", auditLog.executedByUserId());

        spec = auditLog.executedAt() == null
                ? spec.bind("executedAt", LocalDateTime.now())
                : spec.bind("executedAt", auditLog.executedAt());

        return spec.map((row, metadata) -> mapRow(row)).one();
    }

    private static final String SELECT_BASE = """
            SELECT audit_log_id, machine_id, affected_table_name, affected_record_id, action_type,
                   action_description, old_data::text AS old_data, new_data::text AS new_data,
                   ip_address, user_agent, executed_by_user_id, executed_at
            FROM machine_audit_logs
            """;

    @Override
    public Flux<MachineAuditLog> findAll() {
        return databaseClient.sql(SELECT_BASE + " ORDER BY executed_at DESC LIMIT 500")
                .map((row, metadata) -> mapRow(row))
                .all();
    }

    @Override
    public Flux<MachineAuditLog> findByMachineId(Integer machineId) {
        return databaseClient.sql(SELECT_BASE + " WHERE machine_id = :machineId ORDER BY executed_at DESC LIMIT 500")
                .bind("machineId", machineId)
                .map((row, metadata) -> mapRow(row))
                .all();
    }

    @Override
    public Flux<MachineAuditLog> findByActionType(String actionType) {
        return databaseClient.sql(SELECT_BASE + " WHERE action_type = :actionType ORDER BY executed_at DESC LIMIT 500")
                .bind("actionType", actionType)
                .map((row, metadata) -> mapRow(row))
                .all();
    }

    @Override
    public Mono<Void> deleteOlderThan(LocalDateTime threshold) {
        return databaseClient.sql("DELETE FROM machine_audit_logs WHERE executed_at < :threshold")
                .bind("threshold", threshold)
                .then();
    }

    private MachineAuditLog mapRow(Row row) {
        return new MachineAuditLog(
                row.get("audit_log_id", Long.class),
                row.get("machine_id", Integer.class),
                row.get("affected_table_name", String.class),
                row.get("affected_record_id", Integer.class),
                row.get("action_type", String.class),
                row.get("action_description", String.class),
                row.get("old_data", String.class),
                row.get("new_data", String.class),
                row.get("ip_address", String.class),
                row.get("user_agent", String.class),
                row.get("executed_by_user_id", Integer.class),
                row.get("executed_at", LocalDateTime.class)
        );
    }
}
