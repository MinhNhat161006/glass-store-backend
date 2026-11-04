package com.glassstore.order.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wire contract shared with catalog-service.
 *
 * <p>Deliberately duplicated rather than imported: depending on the catalog jar would drag
 * its JPA entities into this service's classpath and couple the two deployments. The JSON
 * shape is the contract, and it mirrors {@code com.glassstore.catalog.dto}.</p>
 */
public final class CatalogContracts {

    private CatalogContracts() {
    }

    public static class ApiEnvelope<T> {
        private int code;
        private String message;
        private String errorCode;
        private T data;
        private String timestamp;

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class VariantInfo {
        private String sku;
        private String barcode;
        private String name;
        private BigDecimal price;
        private String status;
        private String productStatus;
        private Integer availableQuantity;
        private Integer stockQuantity;
        private Integer reservedQuantity;
        private Integer nonSellableQuantity;
        private String imageUrl;

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getProductStatus() { return productStatus; }
        public void setProductStatus(String productStatus) { this.productStatus = productStatus; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
        public Integer getReservedQuantity() { return reservedQuantity; }
        public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
        public Integer getNonSellableQuantity() { return nonSellableQuantity; }
        public void setNonSellableQuantity(Integer nonSellableQuantity) { this.nonSellableQuantity = nonSellableQuantity; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public static class ReservationLine {
        private String sku;
        private Integer quantity;

        public ReservationLine() { }
        public ReservationLine(String sku, Integer quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class ReserveRequest {
        private String reservationKey;
        private String orderCode;
        private List<ReservationLine> lines;
        private String actor;

        public String getReservationKey() { return reservationKey; }
        public void setReservationKey(String reservationKey) { this.reservationKey = reservationKey; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public List<ReservationLine> getLines() { return lines; }
        public void setLines(List<ReservationLine> lines) { this.lines = lines; }
        public String getActor() { return actor; }
        public void setActor(String actor) { this.actor = actor; }
    }

    public static class ReleaseRequest {
        private String reservationKey;
        private String actor;
        private String reason;

        public String getReservationKey() { return reservationKey; }
        public void setReservationKey(String reservationKey) { this.reservationKey = reservationKey; }
        public String getActor() { return actor; }
        public void setActor(String actor) { this.actor = actor; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CommitRequest {
        private String reservationKey;
        private String actor;

        public String getReservationKey() { return reservationKey; }
        public void setReservationKey(String reservationKey) { this.reservationKey = reservationKey; }
        public String getActor() { return actor; }
        public void setActor(String actor) { this.actor = actor; }
    }

    public static class ReservationResult {
        private String reservationKey;
        private String status;
        private boolean replayed;
        private List<ReservationLine> lines;

        public String getReservationKey() { return reservationKey; }
        public void setReservationKey(String reservationKey) { this.reservationKey = reservationKey; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isReplayed() { return replayed; }
        public void setReplayed(boolean replayed) { this.replayed = replayed; }
        public List<ReservationLine> getLines() { return lines; }
        public void setLines(List<ReservationLine> lines) { this.lines = lines; }
    }
}