package com.glassstore.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Request/response payloads for checkout and POS (SRS §4.3, §4.9). */
public final class OrderDtos {

    private OrderDtos() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckoutLine {
        @NotBlank(message = "SKU không được để trống")
        @com.fasterxml.jackson.annotation.JsonAlias({"skuId", "sku_id"})
        private String sku;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;

        /**
         * Price the client believes it is paying. Used only to detect a change and ask the
         * customer to confirm (EX-ORD-02); it is never used as the charged amount.
         */
        private BigDecimal expectedUnitPrice;
        private String productName;
        private Long prescriptionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckoutRequest {
        @NotBlank(message = "Tên khách hàng không được để trống")
        private String customerName;

        /** Walk-in POS customers may omit a phone; online checkout still requires it. */
        private String customerPhone;

        private String shippingAddress;

        private Long customerId;

        @NotEmpty(message = "Đơn hàng phải có ít nhất một sản phẩm")
        @Valid
        private List<CheckoutLine> items;

        @NotBlank(message = "Phương thức thanh toán không được để trống")
        private String paymentMethod;

        private String channel;

        private String orderType;

        private String voucherCode;

        private Long prescriptionId;

        private String staffName;

        private String branchName;

        private String branchCode;
        private Long pointsToUse;
        private BigDecimal cashReceived;

        private String notes;

        private Long posSessionId;

        /**
         * Customer accepts the new price after EX-ORD-02 was raised. Without this flag a
         * changed price is a hard failure.
         */
        private boolean acceptPriceChange;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateRequest {
        @NotBlank(message = "Trạng thái không được để trống")
        private String status;

        private String reason;

        private String actor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelRequest {
        private String reason;
    }

    /** Priced basket, computed server-side before anything is persisted. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricedBasket {
        private List<OrderItemSnapshot> lines;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        /** Non-null when the client's expected price no longer matches the catalog. */
        private List<PriceChange> priceChanges;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemSnapshot {
        private String sku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceChange {
        private String sku;
        private BigDecimal expectedUnitPrice;
        private BigDecimal currentUnitPrice;
    }
}
