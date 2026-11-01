package com.smartinventory.exception;

/**
 * Exception thrown when a stock-out or allocation request exceeds available eligible inventory.
 */
public class InsufficientStockException extends ValidationException {

    private final int requested;
    private final int available;

    public InsufficientStockException(String message) {
        super(message);
        this.requested = 0;
        this.available = 0;
    }

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format("Insufficient stock for '%s'. Requested: %d, Available: %d.",
                productName, requested, available));
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
