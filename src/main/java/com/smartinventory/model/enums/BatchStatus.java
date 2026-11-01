package com.smartinventory.model.enums;

/**
 * Status enumeration for physical inventory batches.
 */
public enum BatchStatus {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    DEPLETED("Depleted");

    private final String displayName;

    BatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
