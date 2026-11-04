package com.glassstore.catalog.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Availability formula: {@code available = actual - reserved - nonSellable} (BR-08).
 *
 * <p>This is the single calculation every oversell guard depends on, so it is verified for
 * the edge cases that break naive implementations.</p>
 */
class ProductVariantAvailabilityTest {

    @Test
    @DisplayName("Available excludes both reserved and non-sellable units")
    void availableExcludesReservedAndBroken() {
        ProductVariant v = ProductVariant.builder()
                .stockQuantity(10)
                .reservedQuantity(3)
                .nonSellableQuantity(2)
                .build();

        assertThat(v.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Availability never goes negative even if counters are inconsistent")
    void neverNegative() {
        ProductVariant v = ProductVariant.builder()
                .stockQuantity(2)
                .reservedQuantity(5)
                .nonSellableQuantity(3)
                .build();

        assertThat(v.getAvailableQuantity()).isZero();
    }

    @Test
    @DisplayName("Null counters are treated as zero rather than throwing")
    void toleratesNullCounters() {
        ProductVariant v = ProductVariant.builder()
                .stockQuantity(7)
                .reservedQuantity(null)
                .nonSellableQuantity(null)
                .build();

        assertThat(v.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Fully reserved stock has no availability")
    void fullyReserved() {
        ProductVariant v = ProductVariant.builder()
                .stockQuantity(4)
                .reservedQuantity(4)
                .nonSellableQuantity(0)
                .build();

        assertThat(v.getAvailableQuantity()).isZero();
    }
}