package com.marketmaker.spring.exception;

/**
 * Thrown when the status API is asked about an order id that doesn't exist
 * (never seen by the ledger or the database). Mapped to a 404 response by
 * {@link GlobalExceptionHandler}.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("No order found with id '" + orderId + "'");
    }
}
