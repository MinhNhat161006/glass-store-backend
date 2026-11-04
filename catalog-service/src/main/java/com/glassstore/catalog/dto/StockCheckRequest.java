package com.glassstore.catalog.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record StockCheckRequest(@NotBlank String branchCode, @NotEmpty List<@Valid Item> items) {
    public record Item(@NotBlank String sku, @NotNull @Positive Integer quantity) {}
}
