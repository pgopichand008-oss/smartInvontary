package com.smartinventory.model.enums;

/**
 * Severity levels for stock deficit alerts.
 */
public enum StockAlertSeverity {
    OUT_OF_STOCK("Out of Stock", 1),
    LOW_STOCK("Low Stock", 2);

    private final String displayName;
    private final int level;

    StockAlertSeverity(String displayName, int level) {
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
