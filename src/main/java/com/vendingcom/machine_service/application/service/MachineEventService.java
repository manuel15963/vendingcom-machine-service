package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.dto.request.CreateMachineEventRequest;
import com.vendingcom.machine_service.application.port.input.MachineEventUseCase;
import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineEventRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineParameterRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineRepositoryPort;
import com.vendingcom.machine_service.domain.exception.BusinessRuleException;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import com.vendingcom.machine_service.domain.model.MachineAuditLog;
import com.vendingcom.machine_service.domain.model.MachineEvent;
import com.vendingcom.machine_service.util.audit.AuditDataSerializer;
import com.vendingcom.machine_service.util.request.RequestContext;
import com.vendingcom.machine_service.util.request.RequestContextFilter;
import com.vendingcom.machine_service.util.security.JwtAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MachineEventService implements MachineEventUseCase {

    private static final String GROUP_MACHINE_STATUS = "MACHINE_STATUS";
    private static final String GROUP_EVENT_TYPE = "EVENT_TYPE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String TABLE_EVENTS = "machine_events";

    private final MachineEventRepositoryPort eventRepositoryPort;
    private final MachineRepositoryPort machineRepositoryPort;
    private final MachineParameterRepositoryPort parameterRepositoryPort;
    private final MachineAuditLogRepositoryPort auditLogRepositoryPort;

    public MachineEventService(
            MachineEventRepositoryPort eventRepositoryPort,
            MachineRepositoryPort machineRepositoryPort,
            MachineParameterRepositoryPort parameterRepositoryPort,
            MachineAuditLogRepositoryPort auditLogRepositoryPort
    ) {
        this.eventRepositoryPort = eventRepositoryPort;
        this.machineRepositoryPort = machineRepositoryPort;
        this.parameterRepositoryPort = parameterRepositoryPort;
        this.auditLogRepositoryPort = auditLogRepositoryPort;
    }

    @Override
    @Transactional
    public Mono<MachineEvent> create(Integer machineId, CreateMachineEventRequest request) {
        return ensureMachineActive(machineId)
                .then(validateEventType(request.eventTypeId()))
                .then(currentActorId())
                .flatMap(actor -> {
                    MachineEvent toSave = new MachineEvent(
                            null,
                            machineId,
                            request.eventTypeId(),
                            normalize(request.title()),
                            normalize(request.description()),
                            actor.orElse(null),
                            LocalDateTime.now(),
                            normalize(request.metadata()),
                            null,
                            null
                    );
                    return eventRepositoryPort.save(toSave)
                            .flatMap(saved -> saveAudit(
                                    "EVENT_ADDED", machineId, saved.eventId(), actor.orElse(null),
                                    "Evento agregado: " + saved.title(),
                                    null, AuditDataSerializer.serialize(saved)
                            ).thenReturn(saved));
                });
    }

    @Override
    public Flux<MachineEvent> findByMachine(Integer machineId) {
        return ensureMachineExists(machineId)
                .thenMany(eventRepositoryPort.findByMachineId(machineId));
    }

    private Mono<Void> ensureMachineExists(Integer machineId) {
        return machineRepositoryPort.findById(machineId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No se encontró la máquina con id: " + machineId)))
                .then();
    }

    /** Para agregar registros la máquina debe existir Y estar activa. */
    private Mono<Void> ensureMachineActive(Integer machineId) {
        return machineRepositoryPort.findById(machineId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No se encontró la máquina con id: " + machineId)))
                .flatMap(machine -> resolveStatusId(STATUS_ACTIVE)
                        .flatMap(activeStatusId -> activeStatusId.equals(machine.machineStatusId())
                                ? Mono.<Void>empty()
                                : Mono.<Void>error(new BusinessRuleException(
                                "MACHINE_INACTIVE", "No se pueden agregar registros a una máquina inactiva."))));
    }

    private Mono<Void> validateEventType(Integer eventTypeId) {
        return parameterRepositoryPort.existsByIdAndGroup(eventTypeId, GROUP_EVENT_TYPE)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.empty()
                        : Mono.error(new BusinessRuleException(
                        "INVALID_EVENT_TYPE",
                        "El tipo de evento indicado no es válido.")));
    }

    private Mono<Integer> resolveStatusId(String statusCode) {
        return parameterRepositoryPort.findIdByGroupAndCode(GROUP_MACHINE_STATUS, statusCode)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                        "MACHINE_STATUS_NOT_CONFIGURED", "No está configurado el estado de máquina: " + statusCode)));
    }

    private Mono<Optional<Integer>> currentActorId() {
        return Mono.deferContextual(ctx -> Mono.just(
                ctx.hasKey(JwtAuthenticationFilter.AUTH_USER_ID_KEY)
                        ? Optional.ofNullable((Integer) ctx.get(JwtAuthenticationFilter.AUTH_USER_ID_KEY))
                        : Optional.<Integer>empty()
        ));
    }

    private Mono<MachineAuditLog> saveAudit(
            String actionType,
            Integer machineId,
            Integer affectedRecordId,
            Integer executedByUserId,
            String description,
            String oldData,
            String newData
    ) {
        return Mono.deferContextual(ctx -> {
            String clientIp = "UNKNOWN";
            String userAgent = "UNKNOWN";
            try {
                RequestContext requestContext = (RequestContext) ctx.get(RequestContextFilter.REQUEST_CONTEXT_KEY);
                clientIp = requestContext.clientIp();
                userAgent = requestContext.userAgent();
            } catch (Exception ignored) {
                // sin contexto reactivo se usa UNKNOWN
            }

            Integer resolvedExecutedBy = executedByUserId;
            if (resolvedExecutedBy == null && ctx.hasKey(JwtAuthenticationFilter.AUTH_USER_ID_KEY)) {
                resolvedExecutedBy = (Integer) ctx.get(JwtAuthenticationFilter.AUTH_USER_ID_KEY);
            }

            MachineAuditLog auditLog = new MachineAuditLog(
                    null,
                    machineId,
                    TABLE_EVENTS,
                    affectedRecordId,
                    actionType,
                    description,
                    oldData,
                    newData,
                    clientIp,
                    userAgent,
                    resolvedExecutedBy,
                    LocalDateTime.now()
            );

            return auditLogRepositoryPort.save(auditLog);
        });
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
