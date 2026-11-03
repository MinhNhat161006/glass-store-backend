package com.glassstore.catalog.service;

import com.glassstore.catalog.dto.StockDtos;

/**
 * Owns every mutation of stock counters (FR-INV-*, BR-08..BR-12).
 *
 * <p>All methods are transactional and lock the affected SKU rows, so two simultaneous
 * checkouts cannot both pass the availability check for the last unit (EX-INV-05).</p>
 */
public interface StockService {

    /** Holds stock for the given lines. All-or-nothing; idempotent on {@code reservationKey}. */
    StockDtos.ReservationResult reserve(StockDtos.ReserveRequest request);

    /** Returns held stock to the pool. Idempotent; unknown keys are a no-op. */
    StockDtos.ReservationResult release(StockDtos.ReleaseRequest request);

    /** Converts a hold into a real deduction. Idempotent. */
    StockDtos.ReservationResult commit(StockDtos.CommitRequest request);

    /**
     * Manual correction by an authorised user (BR-12). Requires a reason.
     *
     * @param delta signed change applied to actual stock
     */
    void adjust(String sku, int delta, String actor, String reason);
}