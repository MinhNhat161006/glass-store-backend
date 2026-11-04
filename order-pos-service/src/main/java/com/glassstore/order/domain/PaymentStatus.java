package com.glassstore.order.domain;

/** Payment transaction lifecycle. Kept separate from order status (SRS §4.6). */
public enum PaymentStatus {

    /** Created, no verified outcome yet. */
    PENDING,
    /** Verified successful charge. At most one per order (BR-15). */
    SUCCESS,
    /** Verified failure. */
    FAILED,
    /** Abandoned/expired before any outcome. */
    CANCELLED,
    /** Order cancelled after payment; refund not yet executed. */
    REFUND_PENDING,
    /** Refund executed. */
    REFUNDED;

    public boolean isSettled() {
        return this == SUCCESS;
    }

    public static PaymentStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        String normalized = raw.trim().toUpperCase();
        for (PaymentStatus s : values()) {
            if (s.name().equals(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + raw);
    }
}