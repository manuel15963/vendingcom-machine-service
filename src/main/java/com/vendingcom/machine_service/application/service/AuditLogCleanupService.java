package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Limpieza periódica de la auditoría (retención). No borra todo: elimina solo los
 * registros más antiguos que la ventana configurada, conservando el historial reciente.
 */
@Service
public class AuditLogCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogCleanupService.class);
    private static final long ONE_WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private final MachineAuditLogRepositoryPort auditLogRepositoryPort;
    private final long retentionDays;

    public AuditLogCleanupService(
            MachineAuditLogRepositoryPort auditLogRepositoryPort,
            @Value("${machine.audit-log.retention-days:30}") long retentionDays
    ) {
        this.auditLogRepositoryPort = auditLogRepositoryPort;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedRate = ONE_WEEK_MILLIS)
    public void cleanOldAuditLogs() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);

        auditLogRepositoryPort.deleteOlderThan(threshold)
                .doOnSuccess(unused -> log.info(
                        "Limpieza de auditoría: eliminados registros anteriores a {}", threshold))
                .onErrorResume(error -> {
                    log.error("Error al limpiar la auditoría", error);
                    return Mono.empty();
                })
                .subscribe();
    }
}
