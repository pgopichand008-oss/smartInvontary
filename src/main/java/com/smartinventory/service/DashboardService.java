package com.smartinventory.service;

import com.smartinventory.dao.DashboardDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.model.AlertSummary;
import com.smartinventory.model.dashboard.CategoryStockStat;
import com.smartinventory.model.dashboard.DashboardMetrics;
import com.smartinventory.model.dashboard.MonthlyMovementStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Service Layer aggregating real-time inventory KPIs and analytics for the executive dashboard.
 */
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final DashboardDAO dashboardDAO;
    private final AlertService alertService;

    public DashboardService() {
        this(new DashboardDAO(), new AlertService());
    }

    public DashboardService(DashboardDAO dashboardDAO, AlertService alertService) {
        this.dashboardDAO = dashboardDAO;
        this.alertService = alertService;
    }

    /**
     * Compiles complete dashboard metrics using real MySQL database records.
     */
    public DashboardMetrics getDashboardMetrics() {
        try {
            int totalProducts = dashboardDAO.getActiveProductsCount();
            int totalCategories = dashboardDAO.getCategoriesCount();
            int totalSuppliers = dashboardDAO.getActiveSuppliersCount();

            BigDecimal[] valuations = dashboardDAO.getInventoryValuations();
            int totalStock = valuations[0].intValue();
            BigDecimal costValuation = valuations[1];
            BigDecimal retailPotential = valuations[2];

            AlertSummary alerts = alertService.getAlertSummary(null, null);
            int expired = alerts.getTotalExpiredBatches();
            int expiringSoon = alerts.getTotalExpiringSoonBatches();
            int lowStock = alerts.getTotalLowStockProducts();
            int outOfStock = alerts.getTotalOutOfStockProducts();

            List<CategoryStockStat> categoryDistributions = dashboardDAO.getCategoryStockDistribution();
            List<MonthlyMovementStat> monthlyMovements = dashboardDAO.getMonthlyMovements(6);

            return new DashboardMetrics(
                    totalProducts,
                    totalStock,
                    costValuation,
                    retailPotential,
                    totalCategories,
                    totalSuppliers,
                    expired,
                    expiringSoon,
                    lowStock,
                    outOfStock,
                    categoryDistributions,
                    monthlyMovements
            );
        } catch (SQLException e) {
            logger.error("Failed to load dashboard metrics: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to load dashboard metrics: " + e.getMessage(), e);
        }
    }
}
