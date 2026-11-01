package com.smartinventory.model;

import com.smartinventory.model.enums.StockAlertSeverity;

/**
 * Domain entity representing a product low-stock or out-of-stock warning.
 */
public class LowStockAlert {

    private final int productId;
    private final String productName;
    private final String sku;
    private final int categoryId;
    private final String categoryName;
    private final String unit;
    private final int minStockLevel;
    private final int currentStock;
    private final int deficit;
    private final StockAlertSeverity severity;

    public LowStockAlert(int productId, String productName, String sku, int categoryId,
                         String categoryName, String unit, int minStockLevel,
                         int currentStock) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.unit = unit != null ? unit : "pcs";
        this.minStockLevel = minStockLevel;
        this.currentStock = currentStock;
        this.deficit = Math.max(0, minStockLevel - currentStock);
        this.severity = (currentStock == 0) ? StockAlertSeverity.OUT_OF_STOCK : StockAlertSeverity.LOW_STOCK;
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

    public String getUnit() {
        return unit;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getDeficit() {
        return deficit;
    }

    public StockAlertSeverity getSeverity() {
        return severity;
    }

    public boolean isOutOfStock() {
        return currentStock == 0;
    }
}
