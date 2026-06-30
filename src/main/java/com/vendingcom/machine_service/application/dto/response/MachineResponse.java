package com.vendingcom.machine_service.application.dto.response;

import com.vendingcom.machine_service.domain.model.Machine;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MachineResponse(
        Integer machineId,
        String code,
        String qrCode,
        Integer customerId,
        Integer locationId,
        String model,
        String brand,
        String serialNumber,
        Integer machineStatusId,
        String machineStatusName,
        Integer machineTypeId,
        String machineTypeName,
        LocalDate installationDate,
        LocalDate lastMaintenanceDate,
        Integer maintenanceIntervalDays,
        String notes,
        Integer createdByUserId,
        Integer updatedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MachineResponse fromDomain(Machine machine) {
        return new MachineResponse(
                machine.machineId(),
                machine.code(),
                machine.qrCode(),
                machine.customerId(),
                machine.locationId(),
                machine.model(),
                machine.brand(),
                machine.serialNumber(),
                machine.machineStatusId(),
                machine.machineStatusName(),
                machine.machineTypeId(),
                machine.machineTypeName(),
                machine.installationDate(),
                machine.lastMaintenanceDate(),
                machine.maintenanceIntervalDays(),
                machine.notes(),
                machine.createdByUserId(),
                machine.updatedByUserId(),
                machine.createdAt(),
                machine.updatedAt()
        );
    }
}
