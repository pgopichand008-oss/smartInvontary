package com.smartinventory.model.report;

/**
 * Enumeration of available reporting types in the system.
 */
public enum ReportType {
    INVENTORY_VALUATION("Inventory Valuation & Stock Summary", "Full stock balance, valuation, and batch breakdown"),
    EXPIRY_SURVEILLANCE("Expiry Surveillance Report", "Quarantined expired batches and 30-day upcoming expirations"),
    LOW_STOCK_REORDER("Low Stock & Reorder Deficit Report", "Active products at or below safety stock threshold"),
    TRANSACTION_AUDIT("Stock Movement Audit Ledger", "Comprehensive historical ledger of all stock transactions");

    private final String title;
    private final String description;

    ReportType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return title;
    }
}
