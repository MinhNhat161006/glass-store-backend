package com.glassstore.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/** Request/response payloads for the internal stock-reservation contract. */
public final class StockDtos {

    private StockDtos() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservationLine {
        @NotBlank(message = "SKU không được để trống")
        private String sku;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReserveRequest {
        /** Idempotency key: replaying the same key returns the existing hold. */
        @NotBlank(message = "reservationKey không được để trống")
        private String reservationKey;

        private String orderCode;

        @NotEmpty(message = "Danh sách SKU không được rỗng")
        @Valid
        private List<ReservationLine> lines;

        private String actor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReleaseRequest {
        @NotBlank(message = "reservationKey không được để trống")
        private String reservationKey;
        private String actor;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommitRequest {
        @NotBlank(message = "reservationKey không được để trống")
        private String reservationKey;
        private String actor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdjustRequest {
        @NotBlank(message = "SKU không được để trống")
        private String sku;
        @NotNull(message = "delta không được để trống")
        private Integer delta;
        private String actor;
        @NotBlank(message = "Điều chỉnh tồn kho bắt buộc phải có lý do")
        private String reason;
    }

    /** Result of a reserve/commit call. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservationResult {
        private String reservationKey;
        private String status;
        /** True when this call found an existing hold and made no new changes. */
        private boolean replayed;
        private List<ReservationLine> lines;
    }
}