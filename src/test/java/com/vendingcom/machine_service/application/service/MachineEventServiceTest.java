package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.dto.request.CreateMachineEventRequest;
import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineEventRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineParameterRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineRepositoryPort;
import com.vendingcom.machine_service.domain.exception.BusinessRuleException;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import com.vendingcom.machine_service.domain.model.Machine;
import com.vendingcom.machine_service.domain.model.MachineEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MachineEventServiceTest {

    private static final String G_STATUS = "MACHINE_STATUS";
    private static final String G_EVENT_TYPE = "EVENT_TYPE";
    private static final Integer ACTIVE_ID = 1;
    private static final Integer INACTIVE_ID = 2;
    private static final Integer EVENT_TYPE_ID = 3;
    private static final Integer MACHINE_ID = 10;

    @Mock private MachineEventRepositoryPort eventRepositoryPort;
    @Mock private MachineRepositoryPort machineRepositoryPort;
    @Mock private MachineParameterRepositoryPort parameterRepositoryPort;
    @Mock private MachineAuditLogRepositoryPort auditLogRepositoryPort;

    @InjectMocks private MachineEventService service;

    private Machine machine(Integer statusId) {
        return new Machine(MACHINE_ID, "VEND-000001", "VEND-000001", 5, 7,
                "Modelo X", "Marca Y", "SN-1", statusId, null, null,
                null, null, null, 0, 1, 1, LocalDateTime.now(), null, "ACTIVE", null);
    }

    private MachineEvent event(Integer id) {
        return new MachineEvent(id, MACHINE_ID, EVENT_TYPE_ID, "Mantenimiento", "Cambio de filtro",
                1, LocalDateTime.now(), null, LocalDateTime.now(), "MAINTENANCE");
    }

    private CreateMachineEventRequest createRequest() {
        return new CreateMachineEventRequest(EVENT_TYPE_ID, "Mantenimiento", "Cambio de filtro", null);
    }

    @BeforeEach
    void setUp() {
        // Stubs "felices" que se ensamblan de forma eager en el flujo de create.
        when(machineRepositoryPort.findById(MACHINE_ID)).thenReturn(Mono.just(machine(ACTIVE_ID)));
        when(parameterRepositoryPort.findIdByGroupAndCode(G_STATUS, "ACTIVE")).thenReturn(Mono.just(ACTIVE_ID));
        when(parameterRepositoryPort.existsByIdAndGroup(EVENT_TYPE_ID, G_EVENT_TYPE)).thenReturn(Mono.just(true));
    }

    @Test
    void create_ok_maquinaActivaYTipoValido() {
        MachineEvent saved = event(20);
        when(eventRepositoryPort.save(any())).thenReturn(Mono.just(saved));
        when(auditLogRepositoryPort.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(MACHINE_ID, createRequest()))
                .expectNextMatches(e -> e.eventId().equals(20) && "MAINTENANCE".equals(e.eventTypeName()))
                .verifyComplete();
    }

    @Test
    void create_maquinaNoExiste_lanzaNotFound() {
        when(machineRepositoryPort.findById(MACHINE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.create(MACHINE_ID, createRequest()))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void create_maquinaInactiva_lanzaBusinessRule() {
        when(machineRepositoryPort.findById(MACHINE_ID)).thenReturn(Mono.just(machine(INACTIVE_ID)));

        StepVerifier.create(service.create(MACHINE_ID, createRequest()))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "MACHINE_INACTIVE".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }

    @Test
    void create_tipoEventoInvalido_lanzaBusinessRule() {
        when(parameterRepositoryPort.existsByIdAndGroup(EVENT_TYPE_ID, G_EVENT_TYPE)).thenReturn(Mono.just(false));

        StepVerifier.create(service.create(MACHINE_ID, createRequest()))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "INVALID_EVENT_TYPE".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }

    @Test
    void findByMachine_maquinaNoExiste_lanzaNotFound() {
        when(machineRepositoryPort.findById(MACHINE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.findByMachine(MACHINE_ID))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void findByMachine_devuelveEventos() {
        when(eventRepositoryPort.findByMachineId(MACHINE_ID)).thenReturn(Flux.just(event(1), event(2)));

        StepVerifier.create(service.findByMachine(MACHINE_ID))
                .expectNextCount(2)
                .verifyComplete();
    }
}
