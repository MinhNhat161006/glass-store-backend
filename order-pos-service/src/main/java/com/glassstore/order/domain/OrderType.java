package com.glassstore.order.domain;

/** What the order contains. Mirrors the existing {@code orders.order_type} column. */
public enum OrderType {
    FRAME_ONLY,
    LENS_ONLY,
    COMPLETE_GLASSES,
    ACCESSORY;

    public static OrderType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMPLETE_GLASSES;
        }
        String normalized = raw.trim().toUpperCase();
        for (OrderType t : values()) {
            if (t.name().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Loại đơn hàng không hợp lệ: " + raw);
    }

    /** Only lens-bearing orders need a prescription and may spawn a lab job. */
    public boolean requiresPrescription() {
        return this == COMPLETE_GLASSES || this == LENS_ONLY;
    }
}