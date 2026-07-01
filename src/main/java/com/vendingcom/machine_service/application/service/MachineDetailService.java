package com.vendingcom.machine_service.application.service;

import com.vendingcom.machine_service.application.dto.response.MachineDetailResponse;
import com.vendingcom.machine_service.application.dto.response.MachineDocumentResponse;
import com.vendingcom.machine_service.application.dto.response.MachineEventResponse;
import com.vendingcom.machine_service.application.dto.response.MachineResponse;
import com.vendingcom.machine_service.application.port.input.MachineDetailUseCase;
import com.vendingcom.machine_service.application.port.output.persistence.MachineDocumentRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineEventRepositoryPort;
import com.vendingcom.machine_service.application.port.output.persistence.MachineRepositoryPort;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MachineDetailService implements MachineDetailUseCase {

    /** Tope defensivo: la ficha muestra a lo sumo los N eventos/documentos más recientes. */
    private static final int MAX_DETAIL_ITEMS = 100;

    private final MachineRepositoryPort machineRepositoryPort;
    private final MachineEventRepositoryPort eventRepositoryPort;
    private final MachineDocumentRepositoryPort documentRepositoryPort;

    public MachineDetailService(
            MachineRepositoryPort machineRepositoryPort,
            MachineEventRepositoryPort eventRepositoryPort,
            MachineDocumentRepositoryPort documentRepositoryPort
    ) {
        this.machineRepositoryPort = machineRepositoryPort;
        this.eventRepositoryPort = eventRepositoryPort;
        this.documentRepositoryPort = documentRepositoryPort;
    }

    @Override
    public Mono<MachineDetailResponse> findDetail(Integer machineId) {
        return machineRepositoryPort.findById(machineId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No se encontró la máquina con id: " + machineId)))
                .flatMap(machine -> Mono.zip(
                        eventRepositoryPort.findByMachineId(machineId)
                                .take(MAX_DETAIL_ITEMS, true)
                                .map(MachineEventResponse::fromDomain).collectList(),
                        documentRepositoryPort.findByMachineId(machineId)
                                .take(MAX_DETAIL_ITEMS, true)
                                .map(MachineDocumentResponse::fromDomain).collectList()
                ).map(tuple -> new MachineDetailResponse(
                        MachineResponse.fromDomain(machine),
                        tuple.getT1(),
                        tuple.getT2()
                )));
    }
}
