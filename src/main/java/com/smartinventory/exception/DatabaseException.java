package com.smartinventory.exception;

/**
 * Wraps checked SQLExceptions into runtime exceptions for cleaner service layers.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
