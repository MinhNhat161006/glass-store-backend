package com.glassstore.catalog.entity;

/** Why a stock movement happened (BR-10: every movement has a source). */
public enum StockMovementType {
    /** Stock held for an order that is not yet paid. */
    RESERVE,
    /** Held stock returned to the pool after cancellation/failure. */
    RELEASE,
    /** Held stock turned into an actual deduction (order paid/settled). */
    COMMIT,
    /** Manual correction by an authorised user (BR-12). */
    ADJUST,
    /** Damage/spoilage: reduces sellable stock (BR-08, EX-INV-04). */
    NON_SELLABLE
}