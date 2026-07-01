package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.dto.request.CreateMachineRequest;
import com.vendingcom.machine_service.application.port.output.client.CustomerValidationPort;
import com.vendingcom.machine_service.application.port.output.client.LocationValidationPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineAuditLogRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineParameterRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineRepositoryPort;
import com.vendingcom.machine_service.domain.exception.BusinessRuleException;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import com.vendingcom.machine_service.domain.model.Machine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MachineServiceTest {

    private static final String G_STATUS = "MACHINE_STATUS";
    private static final Integer ACTIVE_ID = 1;
    private static final Integer INACTIVE_ID = 2;
    private static final Integer CUSTOMER_ID = 5;
    private static final Integer LOCATION_ID = 7;

    @Mock private MachineRepositoryPort machineRepositoryPort;
    @Mock private MachineParameterRepositoryPort parameterRepositoryPort;
    @Mock private MachineAuditLogRepositoryPort auditLogRepositoryPort;
    @Mock private CustomerValidationPort customerValidationPort;
    @Mock private LocationValidationPort locationValidationPort;
    @Mock private TransactionalOperator transactionalOperator;

    @InjectMocks private MachineService service;

    private Machine machine(Integer id, Integer statusId) {
        return new Machine(id, "VEND-000001", "VEND-000001", CUSTOMER_ID, LOCATION_ID,
                "Modelo X", "Marca Y", "SN-1", statusId, null, null,
                null, null, null, 0, 1, 1, LocalDateTime.now(), null, "ACTIVE", null);
    }

    private CreateMachineRequest createRequest() {
        return new CreateMachineRequest(CUSTOMER_ID, LOCATION_ID, "Modelo X", "Marca Y",
                "SN-1", null, null, null, null, null, null);
    }

    @BeforeEach
    void setUp() {
        // Stubs "felices" que se ensamblan de forma eager en el flujo de create.
        when(customerValidationPort.customerExists(CUSTOMER_ID)).thenReturn(Mono.just(true));
        when(locationValidationPort.locationExists(LOCATION_ID)).thenReturn(Mono.just(true));
        when(parameterRepositoryPort.findIdByGroupAndCode(G_STATUS, "ACTIVE")).thenReturn(Mono.just(ACTIVE_ID));
        // El TransactionalOperator solo envuelve el flujo: en el test se comporta como passthrough.
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_ok_validaClienteUbicacionYDevuelveMaquina() {
        Machine saved = machine(10, ACTIVE_ID);
        when(machineRepositoryPort.create(any())).thenReturn(Mono.just(saved));
        when(auditLogRepositoryPort.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(createRequest()))
                .expectNextMatches(m -> m.machineId().equals(10) && "ACTIVE".equals(m.machineStatusName()))
                .verifyComplete();
    }

    @Test
    void create_clienteNoExiste_lanzaCustomerNotFound() {
        when(customerValidationPort.customerExists(CUSTOMER_ID)).thenReturn(Mono.just(false));

        StepVerifier.create(service.create(createRequest()))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "CUSTOMER_NOT_FOUND".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }

    @Test
    void create_ubicacionNoExiste_lanzaLocationNotFound() {
        when(locationValidationPort.locationExists(LOCATION_ID)).thenReturn(Mono.just(false));

        StepVerifier.create(service.create(createRequest()))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "LOCATION_NOT_FOUND".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }

    @Test
    void create_ultimoMantenimientoAnteriorAInstalacion_lanzaBusinessRule() {
        CreateMachineRequest req = new CreateMachineRequest(
                CUSTOMER_ID, LOCATION_ID, "Modelo X", "Marca Y", "SN-1",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 5),
                null, null, null, null);

        StepVerifier.create(service.create(req))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "INVALID_MAINTENANCE_DATE".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }

    @Test
    void changeStatus_tipoInvalido_lanzaBusinessRule() {
        when(parameterRepositoryPort.findIdByGroupAndCode(G_STATUS, "FOO")).thenReturn(Mono.empty());
        // changeStatusInternal se ensambla de forma eager: findById se invoca al construir el flujo.
        when(machineRepositoryPort.findById(10)).thenReturn(Mono.just(machine(10, ACTIVE_ID)));

        StepVerifier.create(service.changeStatus(10, "FOO"))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "INVALID_MACHINE_STATUS".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }

    @Test
    void findById_noExiste_lanzaNotFound() {
        when(machineRepositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(99))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void search_devuelvePaginado() {
        when(machineRepositoryPort.search(any(), any(), any(), any(), eq(20), eq(0L)))
                .thenReturn(Flux.just(machine(1, ACTIVE_ID), machine(2, ACTIVE_ID)));
        when(machineRepositoryPort.countSearch(any(), any(), any(), any())).thenReturn(Mono.just(2L));

        StepVerifier.create(service.search(null, null, null, null, 0, 20))
                .expectNextMatches(page -> page.content().size() == 2
                        && page.totalElements() == 2 && page.totalPages() == 1)
                .verifyComplete();
    }

    @Test
    void deactivate_yaInactivo_lanzaUnchanged() {
        when(machineRepositoryPort.findById(5)).thenReturn(Mono.just(machine(5, INACTIVE_ID)));
        when(parameterRepositoryPort.findIdByGroupAndCode(G_STATUS, "INACTIVE")).thenReturn(Mono.just(INACTIVE_ID));

        StepVerifier.create(service.deactivate(5))
                .expectErrorMatches(e -> e instanceof BusinessRuleException
                        && "MACHINE_STATUS_UNCHANGED".equals(((BusinessRuleException) e).getCode()))
                .verify();
    }
}
