package com.marketmaker.spring.exception;

import com.marketmaker.spring.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates exceptions thrown anywhere in the API layer into a consistent
 * JSON {@link ErrorResponse} instead of Spring's default HTML/blank error
 * page, so Postman (or any client) always gets a clear, structured reason
 * for a failure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Triggered by @Valid failures on the request body (missing/invalid fields). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Validation failed", details);
        return ResponseEntity.badRequest().body(body);
    }

    /** Triggered when the time field is a valid string but not a parseable HH:mm time. */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleBadTime(DateTimeParseException ex) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Invalid time format", Arrays.asList(ex.getMessage()));
        return ResponseEntity.badRequest().body(body);
    }

    /** Triggered by our own input checks (e.g. malformed order id prefix). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadInput(IllegalArgumentException ex) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), ex.getMessage(), Collections.<String>emptyList());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Wraps database errors surfaced from {@code OrderService}. A duplicate
     * order id (same primary key submitted twice) is reported as 409
     * Conflict; anything else as 500.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleDbError(IllegalStateException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SQLIntegrityConstraintViolationException) {
            ErrorResponse body = new ErrorResponse(
                    HttpStatus.CONFLICT.value(), "An order with this id already exists", Collections.<String>emptyList());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database error: " + (cause != null ? cause.getMessage() : ex.getMessage()), Collections.<String>emptyList());
        return ResponseEntity.internalServerError().body(body);
    }

    /** Triggered by the status API when the given order id doesn't exist. */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(), ex.getMessage(), Collections.<String>emptyList());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** Triggered by PUT/DELETE when the order isn't in an eligible state (e.g. already FILLED). */
    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidOrderStateException ex) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(), ex.getMessage(), Collections.<String>emptyList());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /** Catch-all for anything unexpected. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error: " + ex.getMessage(), Collections.<String>emptyList());
        return ResponseEntity.internalServerError().body(body);
    }
}
