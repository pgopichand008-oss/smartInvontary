package com.smartinventory.exception;

/**
 * Thrown when an entity violates uniqueness constraints (e.g. unique name, email, or phone).
 */
public class DuplicateResourceException extends ValidationException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
