package com.smartinventory.exception;

/**
 * Thrown when business validation fails for user input.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
