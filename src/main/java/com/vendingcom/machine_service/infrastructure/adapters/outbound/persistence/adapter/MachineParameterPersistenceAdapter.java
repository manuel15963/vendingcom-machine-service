package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.adapter;

import com.vendingcom.machine_service.application.port.output.persistence.MachineParameterRepositoryPort;
import com.vendingcom.machine_service.domain.model.MachineParameter;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MachineParameterPersistenceAdapter implements MachineParameterRepositoryPort {

    private final DatabaseClient databaseClient;

    public MachineParameterPersistenceAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Integer> findIdByGroupAndCode(String parameterGroup, String parameterCode) {
        return databaseClient.sql("""
                        SELECT parameter_id
                        FROM machine_parameters
                        WHERE parameter_group = :group
                          AND parameter_code  = :code
                          AND parameter_status = 1
                        """)
                .bind("group", parameterGroup)
                .bind("code", parameterCode)
                .map((row, metadata) -> row.get("parameter_id", Integer.class))
                .one();
    }

    @Override
    public Mono<Boolean> existsByIdAndGroup(Integer parameterId, String parameterGroup) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM machine_parameters
                            WHERE parameter_id = :id AND parameter_group = :group AND parameter_status = 1
                        ) AS exists
                        """)
                .bind("id", parameterId)
                .bind("group", parameterGroup)
                .map((row, metadata) -> row.get("exists", Boolean.class))
                .one();
    }

    @Override
    public Mono<String> findCodeById(Integer parameterId) {
        return databaseClient.sql("SELECT parameter_code FROM machine_parameters WHERE parameter_id = :id")
                .bind("id", parameterId)
                .map((row, metadata) -> row.get("parameter_code", String.class))
                .one();
    }

    @Override
    public Flux<MachineParameter> findActiveByGroup(String parameterGroup) {
        return databaseClient.sql("""
                        SELECT parameter_id, parameter_group, parameter_code, parameter_value,
                               description, sort_order, parameter_status
                        FROM machine_parameters
                        WHERE parameter_group = :group AND parameter_status = 1
                        ORDER BY sort_order
                        """)
                .bind("group", parameterGroup)
                .map((row, metadata) -> toDomain(row))
                .all();
    }

    @Override
    public Flux<MachineParameter> findAllActive() {
        return databaseClient.sql("""
                        SELECT parameter_id, parameter_group, parameter_code, parameter_value,
                               description, sort_order, parameter_status
                        FROM machine_parameters
                        WHERE parameter_status = 1
                        ORDER BY parameter_group, sort_order
                        """)
                .map((row, metadata) -> toDomain(row))
                .all();
    }

    private MachineParameter toDomain(Row row) {
        return new MachineParameter(
                row.get("parameter_id", Integer.class),
                row.get("parameter_group", String.class),
                row.get("parameter_code", String.class),
                row.get("parameter_value", String.class),
                row.get("description", String.class),
                row.get("sort_order", Integer.class),
                row.get("parameter_status", Integer.class)
        );
    }
}
