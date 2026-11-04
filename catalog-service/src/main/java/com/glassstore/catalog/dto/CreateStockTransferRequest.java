package com.glassstore.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateStockTransferRequest {

    @NotBlank
    private String sourceLocationCode;

    @NotBlank
    private String destinationLocationCode;

    @Size(max = 500)
    private String reason;

    @NotEmpty
    private List<@Valid Item> items;

    @Data
    public static class Item {
        @NotBlank
        private String sku;

        @NotNull
        @Positive
        private Integer quantity;
    }
}
