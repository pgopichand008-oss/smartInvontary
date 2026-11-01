package com.smartinventory.model;

import com.smartinventory.model.enums.ExpirySeverity;

import java.time.LocalDate;

/**
 * Domain entity representing an active lot expiration warning.
 */
public class ExpiryAlert {

    private final int batchId;
    private final String batchNumber;
    private final int productId;
    private final String productName;
    private final String sku;
    private final int categoryId;
    private final String categoryName;
    private final String supplierName;
    private final int currentQuantity;
    private final String unit;
    private final LocalDate expiryDate;
    private final long daysRemaining;
    private final ExpirySeverity severity;

    public ExpiryAlert(int batchId, String batchNumber, int productId, String productName,
                       String sku, int categoryId, String categoryName, String supplierName,
                       int currentQuantity, String unit, LocalDate expiryDate,
                       long daysRemaining, ExpirySeverity severity) {
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.supplierName = supplierName;
        this.currentQuantity = currentQuantity;
        this.unit = unit != null ? unit : "pcs";
        this.expiryDate = expiryDate;
        this.daysRemaining = daysRemaining;
        this.severity = severity;
    }

    public int getBatchId() {
        return batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public ExpirySeverity getSeverity() {
        return severity;
    }

    public boolean isExpired() {
        return daysRemaining < 0;
    }

    public String getFormattedDaysRemaining() {
        if (daysRemaining < 0) {
            return Math.abs(daysRemaining) + " day(s) ago";
        } else if (daysRemaining == 0) {
            return "Expires today";
        } else {
            return daysRemaining + " day(s) left";
        }
    }
}
