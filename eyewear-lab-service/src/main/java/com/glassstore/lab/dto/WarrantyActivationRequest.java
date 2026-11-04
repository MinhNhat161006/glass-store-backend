package com.glassstore.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record WarrantyActivationRequest(
        @NotNull Long orderId,
        @NotNull Long orderItemId,
        @NotBlank @Pattern(regexp = "FRAME_ADJUSTMENT|LENS_COATING|EXCHANGE_30_DAYS") String benefitType
) {
}
