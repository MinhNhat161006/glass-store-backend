package com.glassstore.lab.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RepairServiceResponse(
        Long id,
        String serviceCode,
        String customerName,
        String customerPhone,
        String productName,
        String productType,
        String productBarcode,
        String frameCondition,
        String lensCondition,
        String reportedProblem,
        String physicalCondition,
        List<String> requestedServices,
        String assignedTechnician,
        String receptionEmployee,
        LocalDateTime receivedAt,
        LocalDateTime expectedPickupAt,
        LocalDateTime completedAt,
        String status,
        String notes,
        String customerNotes,
        List<RepairServiceStatusHistoryDTO> statusHistory
) {
}
