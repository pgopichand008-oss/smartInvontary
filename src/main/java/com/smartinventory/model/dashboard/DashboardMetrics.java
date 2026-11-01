package com.smartinventory.model.dashboard;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated KPI metrics and chart datasets for the main dashboard.
 */
public class DashboardMetrics {

    private final int totalProducts;
    private final int totalAvailableStock;
    private final BigDecimal totalInventoryValuation;
    private final BigDecimal totalRetailPotential;
    private final int totalCategories;
    private final int totalSuppliers;
    private final int totalExpiredBatches;
    private final int totalExpiringSoonBatches;
    private final int totalLowStockProducts;
    private final int totalOutOfStockProducts;
    private final List<CategoryStockStat> categoryDistributions;
    private final List<MonthlyMovementStat> monthlyMovements;

    public DashboardMetrics(int totalProducts, int totalAvailableStock,
                            BigDecimal totalInventoryValuation, BigDecimal totalRetailPotential,
                            int totalCategories, int totalSuppliers,
                            int totalExpiredBatches, int totalExpiringSoonBatches,
                            int totalLowStockProducts, int totalOutOfStockProducts,
                            List<CategoryStockStat> categoryDistributions,
                            List<MonthlyMovementStat> monthlyMovements) {
        this.totalProducts = totalProducts;
        this.totalAvailableStock = totalAvailableStock;
        this.totalInventoryValuation = totalInventoryValuation != null ? totalInventoryValuation : BigDecimal.ZERO;
        this.totalRetailPotential = totalRetailPotential != null ? totalRetailPotential : BigDecimal.ZERO;
        this.totalCategories = totalCategories;
        this.totalSuppliers = totalSuppliers;
        this.totalExpiredBatches = totalExpiredBatches;
        this.totalExpiringSoonBatches = totalExpiringSoonBatches;
        this.totalLowStockProducts = totalLowStockProducts;
        this.totalOutOfStockProducts = totalOutOfStockProducts;
        this.categoryDistributions = categoryDistributions != null ? categoryDistributions : Collections.emptyList();
        this.monthlyMovements = monthlyMovements != null ? monthlyMovements : Collections.emptyList();
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getTotalAvailableStock() {
        return totalAvailableStock;
    }

    public int getTotalStock() {
        return totalAvailableStock;
    }

    public BigDecimal getTotalInventoryValuation() {
        return totalInventoryValuation;
    }

    public BigDecimal getTotalCostValuation() {
        return totalInventoryValuation;
    }

    public BigDecimal getTotalRetailPotential() {
        return totalRetailPotential;
    }

    public int getTotalCategories() {
        return totalCategories;
    }

    public int getTotalSuppliers() {
        return totalSuppliers;
    }

    public int getTotalExpiredBatches() {
        return totalExpiredBatches;
    }

    public int getExpiredBatches() {
        return totalExpiredBatches;
    }

    public int getTotalExpiringSoonBatches() {
        return totalExpiringSoonBatches;
    }

    public int getExpiringSoonBatches() {
        return totalExpiringSoonBatches;
    }

    public int getTotalLowStockProducts() {
        return totalLowStockProducts;
    }

    public int getLowStockProducts() {
        return totalLowStockProducts;
    }

    public int getTotalOutOfStockProducts() {
        return totalOutOfStockProducts;
    }

    public int getOutOfStockProducts() {
        return totalOutOfStockProducts;
    }

    public List<CategoryStockStat> getCategoryDistributions() {
        return categoryDistributions;
    }

    public List<CategoryStockStat> getCategoryDistribution() {
        return categoryDistributions;
    }

    public List<MonthlyMovementStat> getMonthlyMovements() {
        return monthlyMovements;
    }
}
