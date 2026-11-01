package com.smartinventory.dao;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.dashboard.CategoryStockStat;
import com.smartinventory.model.dashboard.MonthlyMovementStat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object computing aggregated inventory KPIs, category distribution,
 * and monthly transaction movements.
 */
public class DashboardDAO {

    public int getActiveProductsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getCategoriesCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getActiveSuppliersCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM suppliers WHERE is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Calculates total available units and inventory financial valuations.
     * Returns array: [BigDecimal totalUnits, BigDecimal totalCostValuation, BigDecimal totalRetailPotential]
     */
    public BigDecimal[] getInventoryValuations() throws SQLException {
        String sql =
                "SELECT COALESCE(SUM(b.current_quantity), 0) AS total_units, " +
                "       COALESCE(SUM(b.current_quantity * b.purchase_price), 0) AS total_cost_value, " +
                "       COALESCE(SUM(b.current_quantity * b.selling_price), 0) AS total_retail_value " +
                "FROM product_batches b " +
                "JOIN products p ON b.product_id = p.product_id " +
                "WHERE p.is_active = TRUE " +
                "  AND b.current_quantity > 0 " +
                "  AND b.status IN ('ACTIVE', 'EXPIRING_SOON') " +
                "  AND b.expiry_date >= CURRENT_DATE";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new BigDecimal[]{
                        BigDecimal.valueOf(rs.getInt("total_units")),
                        rs.getBigDecimal("total_cost_value"),
                        rs.getBigDecimal("total_retail_value")
                };
            }
        }
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    /**
     * Retrieves category-wise stock volume distribution.
     */
    public List<CategoryStockStat> getCategoryStockDistribution() throws SQLException {
        String sql =
                "SELECT c.category_id, c.category_name, " +
                "       COALESCE(SUM(CASE " +
                "           WHEN b.current_quantity > 0 " +
                "            AND b.status IN ('ACTIVE', 'EXPIRING_SOON') " +
                "            AND b.expiry_date >= CURRENT_DATE " +
                "           THEN b.current_quantity " +
                "           ELSE 0 " +
                "       END), 0) AS category_stock " +
                "FROM categories c " +
                "LEFT JOIN products p ON c.category_id = p.category_id AND p.is_active = TRUE " +
                "LEFT JOIN product_batches b ON p.product_id = b.product_id " +
                "GROUP BY c.category_id, c.category_name " +
                "ORDER BY category_stock DESC, c.category_name ASC";

        List<CategoryStockStat> rawList = new ArrayList<>();
        int grandTotal = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int stock = rs.getInt("category_stock");
                grandTotal += stock;
                rawList.add(new CategoryStockStat(
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        stock,
                        0.0
                ));
            }
        }

        // Compute percentages
        List<CategoryStockStat> result = new ArrayList<>();
        for (CategoryStockStat stat : rawList) {
            double pct = (grandTotal > 0) ? ((double) stat.getTotalQuantity() / grandTotal) * 100.0 : 0.0;
            result.add(new CategoryStockStat(stat.getCategoryId(), stat.getCategoryName(), stat.getTotalQuantity(), pct));
        }

        return result;
    }

    /**
     * Retrieves monthly stock movements for the last N months.
     */
    public List<MonthlyMovementStat> getMonthlyMovements(int months) throws SQLException {
        String sql =
                "SELECT DATE_FORMAT(t.created_at, '%Y-%m') AS period, " +
                "       COALESCE(SUM(CASE WHEN t.transaction_type IN ('PURCHASE_IN', 'RETURN_IN') THEN t.quantity ELSE 0 END), 0) AS inflow, " +
                "       COALESCE(SUM(CASE WHEN t.transaction_type = 'SALE_OUT' THEN t.quantity ELSE 0 END), 0) AS outflow, " +
                "       COALESCE(SUM(CASE WHEN t.transaction_type IN ('DAMAGE_OUT', 'EXPIRED_WRITE_OFF') THEN t.quantity ELSE 0 END), 0) AS discards " +
                "FROM stock_transactions t " +
                "WHERE t.created_at >= DATE_SUB(CURRENT_DATE, INTERVAL ? MONTH) " +
                "GROUP BY period " +
                "ORDER BY period ASC";

        List<MonthlyMovementStat> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, months));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new MonthlyMovementStat(
                            rs.getString("period"),
                            rs.getInt("inflow"),
                            rs.getInt("outflow"),
                            rs.getInt("discards")
                    ));
                }
            }
        }
        return list;
    }
}
