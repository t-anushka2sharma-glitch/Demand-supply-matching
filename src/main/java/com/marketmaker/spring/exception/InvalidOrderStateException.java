package com.marketmaker.spring.exception;

/**
 * Thrown when a PUT or DELETE is attempted on an order that's no longer in
 * an eligible state for that operation — e.g. trying to edit an order
 * that's already been (partially) matched, or cancel one that's already
 * FILLED or CANCELLED. Translated to a {@code 409 Conflict} by
 * {@link GlobalExceptionHandler}.
 */
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
