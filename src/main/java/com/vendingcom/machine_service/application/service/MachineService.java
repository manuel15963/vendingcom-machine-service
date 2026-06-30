package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.dto.request.CreateMachineRequest;
import com.vendingcom.machine_service.application.dto.request.UpdateMachineRequest;
import com.vendingcom.machine_service.application.dto.response.PagedResponse;
import com.vendingcom.machine_service.application.port.input.MachineUseCase;
import com.vendingcom.machine_service.application.port.output.client.CustomerValidationPort;
import com.vendingcom.machine_service.application.port.output.client.LocationValidationPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineParameterRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineRepositoryPort;
import com.vendingcom.machine_service.domain.exception.BusinessRuleException;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import com.vendingcom.machine_service.domain.model.Machine;
import com.vendingcom.machine_service.domain.model.MachineAuditLog;
import com.vendingcom.machine_service.util.audit.AuditDataSerializer;
import com.vendingcom.machine_service.util.request.RequestContext;
import com.vendingcom.machine_service.util.request.RequestContextFilter;
import com.vendingcom.machine_service.util.security.JwtAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MachineService implements MachineUseCase {

    private static final String GROUP_MACHINE_STATUS = "MACHINE_STATUS";
    private static final String GROUP_MACHINE_TYPE = "MACHINE_TYPE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String TABLE_MACHINES = "machines";

    private final MachineRepositoryPort machineRepositoryPort;
    private final MachineParameterRepositoryPort machineParameterRepositoryPort;
    private final MachineAuditLogRepositoryPort machineAuditLogRepositoryPort;
    private final CustomerValidationPort customerValidationPort;
    private final LocationValidationPort locationValidationPort;
    private final TransactionalOperator transactionalOperator;

    public MachineService(
            MachineRepositoryPort machineRepositoryPort,
            MachineParameterRepositoryPort machineParameterRepositoryPort,
            MachineAuditLogRepositoryPort machineAuditLogRepositoryPort,
            CustomerValidationPort customerValidationPort,
            LocationValidationPort locationValidationPort,
            TransactionalOperator transactionalOperator
    ) {
        this.machineRepositoryPort = machineRepositoryPort;
        this.machineParameterRepositoryPort = machineParameterRepositoryPort;
        this.machineAuditLogRepositoryPort = machineAuditLogRepositoryPort;
        this.customerValidationPort = customerValidationPort;
        this.locationValidationPort = locationValidationPort;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<Machine> create(CreateMachineRequest request) {
        // Las validaciones entre microservicios (HTTP) se hacen FUERA de la transacción para no
        // mantener abierta una conexión R2DBC durante la E/S remota. Solo tras validar cliente y
        // ubicación se abre la transacción que cubre resolveStatusId + create + auditoría.
        return validateCustomer(request.customerId())
                .then(validateLocation(request.locationId()))
                .then(validateMachineTypeIfPresent(request.machineTypeId()))
                .then(persistNewMachine(request));
    }

    /**
     * Solo las operaciones de BD (resolveStatusId + create + auditoría) viajan dentro de la
     * transacción. Se usa TransactionalOperator en vez de @Transactional porque este método se
     * invoca desde create() del mismo bean (la auto-invocación NO pasa por el proxy de Spring,
     * así que @Transactional aquí no tendría efecto).
     */
    private Mono<Machine> persistNewMachine(CreateMachineRequest request) {
        return resolveStatusId(STATUS_ACTIVE)
                .flatMap(statusId -> currentActorId()
                        .flatMap(actor -> {
                            Machine toSave = new Machine(
                                    null,
                                    null,                       // code: lo genera el trigger
                                    normalize(request.qrCode()), // qr_code: si es null el trigger lo iguala al code
                                    request.customerId(),
                                    request.locationId(),
                                    normalize(request.model()),
                                    normalize(request.brand()),
                                    normalize(request.serialNumber()),
                                    statusId,
                                    request.machineTypeId(),
                                    request.installationDate(),
                                    request.lastMaintenanceDate(),
                                    request.maintenanceIntervalDays(),
                                    normalize(request.notes()),
                                    null,                       // version: la BD lo inicializa en 0
                                    actor.orElse(null),
                                    null, null, null,
                                    null, null
                            );

                            return machineRepositoryPort.create(toSave)
                                    .flatMap(saved -> saveAudit(
                                            "MACHINE_CREATED",
                                            saved.machineId(),
                                            actor.orElse(null),
                                            "Máquina creada: " + saved.code(),
                                            null,
                                            AuditDataSerializer.serializeMachine(saved)
                                    ).thenReturn(saved));
                        }))
                .as(transactionalOperator::transactional);
    }

    @Override
    @Transactional
    public Mono<Machine> update(Integer machineId, UpdateMachineRequest request) {
        return validateMachineTypeIfPresent(request.machineTypeId())
                .then(machineRepositoryPort.findById(machineId)
                .switchIfEmpty(notFound(machineId))
                .flatMap(existing -> currentActorId()
                        .flatMap(actor -> {
                            Machine toUpdate = new Machine(
                                    existing.machineId(),
                                    existing.code(),
                                    existing.qrCode(),
                                    existing.customerId(),
                                    existing.locationId(),
                                    normalize(request.model()),
                                    normalize(request.brand()),
                                    normalize(request.serialNumber()),
                                    existing.machineStatusId(),
                                    request.machineTypeId(),
                                    request.installationDate(),
                                    request.lastMaintenanceDate(),
                                    request.maintenanceIntervalDays(),
                                    normalize(request.notes()),
                                    existing.version(),
                                    existing.createdByUserId(),
                                    actor.orElse(null),
                                    existing.createdAt(),
                                    null,   // updated_at lo fija el trigger; no se envía desde la app
                                    null, null
                            );

                            return machineRepositoryPort.update(toUpdate)
                                    .flatMap(updated -> saveAudit(
                                            "MACHINE_UPDATED",
                                            updated.machineId(),
                                            actor.orElse(null),
                                            "Máquina actualizada: " + updated.code(),
                                            AuditDataSerializer.serializeMachine(existing),
                                            AuditDataSerializer.serializeMachine(updated)
                                    ).thenReturn(updated));
                        })));
    }

    @Override
    public Mono<Machine> findById(Integer machineId) {
        return machineRepositoryPort.findById(machineId)
                .switchIfEmpty(notFound(machineId));
    }

    @Override
    public Mono<PagedResponse<Machine>> search(String search, Integer customerId, Integer locationId, Integer statusId, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        long offset = (long) safePage * safeSize;
        String term = (search == null || search.isBlank()) ? null : search.trim();

        return machineRepositoryPort.search(term, customerId, locationId, statusId, safeSize, offset)
                .collectList()
                .zipWith(machineRepositoryPort.countSearch(term, customerId, locationId, statusId))
                .map(tuple -> PagedResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2()));
    }

    @Override
    @Transactional
    public Mono<Machine> activate(Integer machineId) {
        return changeStatusInternal(machineId, STATUS_ACTIVE, "MACHINE_ACTIVATED", "Máquina activada: ");
    }

    @Override
    @Transactional
    public Mono<Void> deactivate(Integer machineId) {
        return changeStatusInternal(machineId, STATUS_INACTIVE, "MACHINE_DEACTIVATED", "Máquina desactivada: ").then();
    }

    @Override
    @Transactional
    public Mono<Machine> changeStatus(Integer machineId, String statusCode) {
        String normalizedCode = statusCode == null ? null : statusCode.trim().toUpperCase();
        return validateStatusCode(normalizedCode)
                .then(changeStatusInternal(machineId, normalizedCode, "STATUS_CHANGE", "Estado de la máquina cambiado: "));
    }

    private Mono<Machine> changeStatusInternal(Integer machineId, String statusCode, String action, String description) {
        return machineRepositoryPort.findById(machineId)
                .switchIfEmpty(notFound(machineId))
                .flatMap(existing -> resolveStatusId(statusCode)
                        .flatMap(statusId -> {
                            if (statusId.equals(existing.machineStatusId())) {
                                return Mono.error(new BusinessRuleException(
                                        "MACHINE_STATUS_UNCHANGED",
                                        "La máquina ya se encuentra en ese estado."
                                ));
                            }
                            return currentActorId().flatMap(actor -> {
                                Machine toUpdate = new Machine(
                                        existing.machineId(),
                                        existing.code(),
                                        existing.qrCode(),
                                        existing.customerId(),
                                        existing.locationId(),
                                        existing.model(),
                                        existing.brand(),
                                        existing.serialNumber(),
                                        statusId,
                                        existing.machineTypeId(),
                                        existing.installationDate(),
                                        existing.lastMaintenanceDate(),
                                        existing.maintenanceIntervalDays(),
                                        existing.notes(),
                                        existing.version(),
                                        existing.createdByUserId(),
                                        actor.orElse(null),
                                        existing.createdAt(),
                                        null,   // updated_at lo fija el trigger; no se envía desde la app
                                        null, null
                                );

                                return machineRepositoryPort.update(toUpdate)
                                        .flatMap(updated -> saveAudit(
                                                action,
                                                updated.machineId(),
                                                actor.orElse(null),
                                                description + updated.code(),
                                                AuditDataSerializer.serializeMachine(existing),
                                                AuditDataSerializer.serializeMachine(updated)
                                        ).thenReturn(updated));
                            });
                        }));
    }

    /** El cliente dueño debe existir en customer-service (validación entre microservicios). */
    private Mono<Void> validateCustomer(Integer customerId) {
        return customerValidationPort.customerExists(customerId)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.empty()
                        : Mono.error(new BusinessRuleException(
                        "CUSTOMER_NOT_FOUND",
                        "El cliente indicado no existe.")));
    }

    /** La ubicación debe existir en location-service (validación entre microservicios). */
    private Mono<Void> validateLocation(Integer locationId) {
        return locationValidationPort.locationExists(locationId)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.empty()
                        : Mono.error(new BusinessRuleException(
                        "LOCATION_NOT_FOUND",
                        "La ubicación indicada no existe.")));
    }

    /** El código de estado debe pertenecer al grupo MACHINE_STATUS. */
    private Mono<Void> validateStatusCode(String statusCode) {
        return machineParameterRepositoryPort.findIdByGroupAndCode(GROUP_MACHINE_STATUS, statusCode)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                        "INVALID_MACHINE_STATUS",
                        "El estado indicado no es válido.")))
                .then();
    }

    /** Si se indica un tipo de máquina, debe existir y pertenecer al grupo MACHINE_TYPE. */
    private Mono<Void> validateMachineTypeIfPresent(Integer machineTypeId) {
        if (machineTypeId == null) {
            return Mono.empty();
        }
        return machineParameterRepositoryPort.existsByIdAndGroup(machineTypeId, GROUP_MACHINE_TYPE)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.<Boolean>empty()
                        : Mono.<Boolean>error(new BusinessRuleException(
                        "INVALID_MACHINE_TYPE",
                        "El tipo de máquina indicado no es válido.")))
                .then();
    }

    private Mono<Integer> resolveStatusId(String statusCode) {
        return machineParameterRepositoryPort.findIdByGroupAndCode(GROUP_MACHINE_STATUS, statusCode)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                        "MACHINE_STATUS_NOT_CONFIGURED",
                        "No está configurado el estado de máquina: " + statusCode)));
    }

    private <T> Mono<T> notFound(Integer machineId) {
        return Mono.error(new ResourceNotFoundException("No se encontró la máquina con id: " + machineId));
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
                    TABLE_MACHINES,
                    machineId,
                    actionType,
                    description,
                    oldData,
                    newData,
                    clientIp,
                    userAgent,
                    resolvedExecutedBy,
                    LocalDateTime.now()
            );

            return machineAuditLogRepositoryPort.save(auditLog);
        });
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
