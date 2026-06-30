package com.vendingcom.machine_service.application.dto.response;

import com.vendingcom.machine_service.domain.model.MachineEvent;

import java.time.LocalDateTime;

public record MachineEventResponse(
        Integer eventId,
        Integer machineId,
        Integer eventTypeId,
        String eventTypeName,
        String title,
        String description,
        Integer performedByUserId,
        LocalDateTime eventDate,
        String metadata,
        LocalDateTime createdAt
) {
    public static MachineEventResponse fromDomain(MachineEvent event) {
        return new MachineEventResponse(
                event.eventId(),
                event.machineId(),
                event.eventTypeId(),
                event.eventTypeName(),
                event.title(),
                event.description(),
                event.performedByUserId(),
                event.eventDate(),
                event.metadata(),
                event.createdAt()
        );
    }
}
