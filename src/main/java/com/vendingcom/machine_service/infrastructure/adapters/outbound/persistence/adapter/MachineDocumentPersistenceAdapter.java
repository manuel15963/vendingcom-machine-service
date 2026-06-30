package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.adapter;

import com.vendingcom.machine_service.application.port.output.persistence.MachineDocumentRepositoryPort;
import com.vendingcom.machine_service.domain.model.MachineDocument;
import com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.mapper.MachineDocumentPersistenceMapper;
import com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.repository.ReactiveMachineDocumentRepository;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class MachineDocumentPersistenceAdapter implements MachineDocumentRepositoryPort {

    // Lectura con JOIN al catálogo para traer la etiqueta legible del tipo de documento.
    private static final String SELECT_WITH_LABELS = """
            SELECT d.document_id, d.machine_id, d.document_type_id, d.file_name, d.file_url,
                   d.file_size, d.mime_type, d.uploaded_by_user_id, d.uploaded_at,
                   t.parameter_value AS document_type_name
            FROM machine_documents d
            LEFT JOIN machine_parameters t ON t.parameter_id = d.document_type_id
            """;

    private final ReactiveMachineDocumentRepository reactiveMachineDocumentRepository;
    private final MachineDocumentPersistenceMapper machineDocumentPersistenceMapper;
    private final DatabaseClient databaseClient;

    public MachineDocumentPersistenceAdapter(
            ReactiveMachineDocumentRepository reactiveMachineDocumentRepository,
            MachineDocumentPersistenceMapper machineDocumentPersistenceMapper,
            DatabaseClient databaseClient
    ) {
        this.reactiveMachineDocumentRepository = reactiveMachineDocumentRepository;
        this.machineDocumentPersistenceMapper = machineDocumentPersistenceMapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<MachineDocument> save(MachineDocument document) {
        return reactiveMachineDocumentRepository.save(machineDocumentPersistenceMapper.toEntity(document))
                .map(machineDocumentPersistenceMapper::toDomain);
    }

    @Override
    public Mono<MachineDocument> findById(Integer documentId) {
        return databaseClient.sql(SELECT_WITH_LABELS + " WHERE d.document_id = :id")
                .bind("id", documentId)
                .map((row, metadata) -> mapRow(row))
                .one();
    }

    @Override
    public Flux<MachineDocument> findByMachineId(Integer machineId) {
        return databaseClient.sql(SELECT_WITH_LABELS + " WHERE d.machine_id = :machineId ORDER BY d.uploaded_at DESC")
                .bind("machineId", machineId)
                .map((row, metadata) -> mapRow(row))
                .all();
    }

    @Override
    public Mono<Void> deleteById(Integer documentId) {
        return reactiveMachineDocumentRepository.deleteById(documentId);
    }

    private MachineDocument mapRow(Row row) {
        return new MachineDocument(
                row.get("document_id", Integer.class),
                row.get("machine_id", Integer.class),
                row.get("document_type_id", Integer.class),
                row.get("file_name", String.class),
                row.get("file_url", String.class),
                row.get("file_size", Long.class),
                row.get("mime_type", String.class),
                row.get("uploaded_by_user_id", Integer.class),
                row.get("uploaded_at", LocalDateTime.class),
                row.get("document_type_name", String.class)
        );
    }
}
