package com.smartinventory.model.enums;

/**
 * Severity levels for product expiration alerts.
 */
public enum ExpirySeverity {
    EXPIRED("Expired", 0),
    EXPIRING_7_DAYS("Within 7 Days", 1),
    EXPIRING_15_DAYS("Within 15 Days", 2),
    EXPIRING_30_DAYS("Within 30 Days", 3);

    private final String displayName;
    private final int level;

    ExpirySeverity(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
