package com.glassstore.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RepairServiceStatusRequest(
        @NotBlank @Size(max = 32) String status,
        @NotBlank @Size(max = 120) String changedBy,
        @Size(max = 1000) String notes
) {
}
