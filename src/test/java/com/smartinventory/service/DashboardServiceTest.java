package com.smartinventory.service;

import com.smartinventory.model.dashboard.CategoryStockStat;
import com.smartinventory.model.dashboard.DashboardMetrics;
import com.smartinventory.model.dashboard.MonthlyMovementStat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated test suite for Phase 7 Executive Dashboard metrics and analytics compilation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DashboardServiceTest {

    private static DashboardService dashboardService;

    @BeforeAll
    public static void setUp() {
        dashboardService = new DashboardService();
    }

    @Test
    @Order(1)
    @DisplayName("Dashboard Metrics: Should compile valid metrics from real MySQL database")
    public void testGetDashboardMetrics() {
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();
        assertNotNull(metrics, "DashboardMetrics must not be null");

        // Verify counts are sane non-negative integers
        assertTrue(metrics.getTotalProducts() >= 0, "Total products should be >= 0");
        assertTrue(metrics.getTotalStock() >= 0, "Total physical stock should be >= 0");
        assertTrue(metrics.getTotalCategories() >= 0, "Total categories should be >= 0");
        assertTrue(metrics.getTotalSuppliers() >= 0, "Total suppliers should be >= 0");

        // Verify valuations
        assertNotNull(metrics.getTotalCostValuation(), "Cost valuation should not be null");
        assertTrue(metrics.getTotalCostValuation().compareTo(BigDecimal.ZERO) >= 0,
                "Cost valuation must be >= 0");
        assertNotNull(metrics.getTotalRetailPotential(), "Retail potential should not be null");
        assertTrue(metrics.getTotalRetailPotential().compareTo(BigDecimal.ZERO) >= 0,
                "Retail potential must be >= 0");

        // Verify alerts aggregate
        assertTrue(metrics.getExpiredBatches() >= 0, "Expired batches count should be >= 0");
        assertTrue(metrics.getExpiringSoonBatches() >= 0, "Expiring soon batches should be >= 0");
        assertTrue(metrics.getLowStockProducts() >= 0, "Low stock products should be >= 0");
        assertTrue(metrics.getOutOfStockProducts() >= 0, "Out of stock products should be >= 0");

        // Verify Category Distribution
        List<CategoryStockStat> catList = metrics.getCategoryDistribution();
        assertNotNull(catList, "Category distributions must not be null");
        for (CategoryStockStat stat : catList) {
            assertNotNull(stat.getCategoryName(), "Category name in stat must not be null");
            assertTrue(stat.getTotalUnits() >= 0, "Category total units must be >= 0");
            assertTrue(stat.getPercentage() >= 0.0 && stat.getPercentage() <= 100.0,
                    "Category percentage must be between 0 and 100");
        }

        // Verify Monthly Movements
        List<MonthlyMovementStat> movements = metrics.getMonthlyMovements();
        assertNotNull(movements, "Monthly movements must not be null");
        for (MonthlyMovementStat m : movements) {
            assertNotNull(m.getMonth(), "Month label must not be null");
            assertTrue(m.getInflow() >= 0, "Inflow must be >= 0");
            assertTrue(m.getOutflow() >= 0, "Outflow must be >= 0");
            assertTrue(m.getDiscards() >= 0, "Discards must be >= 0");
        }
    }
}
