package com.smartinventory.model.dashboard;

/**
 * Metric data for Category-wise stock volume and distribution.
 */
public class CategoryStockStat {

    private final int categoryId;
    private final String categoryName;
    private final int totalQuantity;
    private final double percentage;

    public CategoryStockStat(int categoryId, String categoryName, int totalQuantity, double percentage) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalQuantity = totalQuantity;
        this.percentage = percentage;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getTotalUnits() {
        return totalQuantity;
    }

    public double getPercentage() {
        return percentage;
    }
}
