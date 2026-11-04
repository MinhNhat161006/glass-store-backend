package com.glassstore.order.domain;

/** Sales channel. Online (web/app) and in-store POS share one order domain (SRS §4.9). */
public enum SalesChannel {
    ONLINE,
    POS;

    public static SalesChannel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ONLINE;
        }
        String normalized = raw.trim().toUpperCase();
        for (SalesChannel c : values()) {
            if (c.name().equals(normalized)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Kênh bán hàng không hợp lệ: " + raw);
    }
}