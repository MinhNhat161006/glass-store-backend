package com.glassstore.catalog.entity;

/** Lifecycle of a stock hold. */
public enum ReservationStatus {
    /** Stock is currently held and still counts against availability. */
    ACTIVE,
    /** Hold converted into a real deduction. */
    COMMITTED,
    /** Hold released back to availability. */
    RELEASED
}