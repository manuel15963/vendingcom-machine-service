package com.vendingcom.machine_service.application.dto.response;

import java.util.List;

/**
 * Vista completa de una máquina con sus eventos y documentos,
 * para construir la ficha en una sola llamada.
 */
public record MachineDetailResponse(
        MachineResponse machine,
        List<MachineEventResponse> events,
        List<MachineDocumentResponse> documents
) {
}
