package com.marketmaker.model;

/**
 * Lifecycle status of an order in the ledger.
 * - PENDING: none of the requested quantity has been matched yet.
 * - PARTIALLY_FILLED: some, but not all, of the quantity has been matched.
 * - FILLED: the entire requested quantity has been matched (order is complete).
 * - CANCELLED: the order (or its remaining unmatched quantity) was withdrawn
 *   by the client before it could be fully matched. Any quantity that had
 *   already been matched into trades before cancellation is unaffected.
 */
public enum OrderStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED
}
