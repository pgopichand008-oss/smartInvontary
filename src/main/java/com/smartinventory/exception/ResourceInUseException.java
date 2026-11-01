package com.smartinventory.exception;

/**
 * Thrown when an entity cannot be deleted because dependent records reference it.
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
