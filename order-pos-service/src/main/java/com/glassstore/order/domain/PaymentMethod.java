package com.glassstore.order.domain;

/**
 * Supported payment methods.
 *
 * <p>Which of these are actually offered to customers is an open business question
 * (OQ-04); the enum only describes what the platform can represent.</p>
 */
public enum PaymentMethod {

    /** Cash at the counter. Settled by an explicit staff action, never by the gateway. */
    CASH,
    /** Bank QR (VietQR / Napas 247). Requires a configured gateway (OQ-05). */
    VIETQR,
    /** Online wallet/aggregator. Requires a configured gateway (OQ-05). */
    VNPAY,
    /** Card at the POS terminal. */
    CARD,
    /** Cash on delivery; only valid when the business enables it (FR-PAY-03, OQ-04). */
    COD;

    /** True when the method needs an external gateway to become SUCCESS. */
    public boolean requiresGateway() {
        return this == VIETQR || this == VNPAY;
    }

    public static PaymentMethod parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Phương thức thanh toán không được để trống");
        }
        String normalized = raw.trim().toUpperCase();
        for (PaymentMethod m : values()) {
            if (m.name().equals(normalized)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + raw);
    }
}