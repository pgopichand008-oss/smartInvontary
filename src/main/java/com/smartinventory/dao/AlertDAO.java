package com.smartinventory.dao;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.ExpiryAlert;
import com.smartinventory.model.LowStockAlert;
import com.smartinventory.model.enums.ExpirySeverity;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for real-time surveillance of expiration boundaries and stock levels.
 */
public class AlertDAO {

    /**
     * Retrieves all expired batches with active non-zero quantity for active products.
     */
    public List<ExpiryAlert> findExpiredBatches(Integer categoryId, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT b.batch_id, b.batch_number, b.current_quantity, b.expiry_date, " +
                "p.product_id, p.product_name, p.sku, p.unit, " +
                "c.category_id, c.category_name, s.supplier_name, " +
                "DATEDIFF(b.expiry_date, CURRENT_DATE) AS days_remaining " +
                "FROM product_batches b " +
                "JOIN products p ON b.product_id = p.product_id " +
                "JOIN categories c ON p.category_id = c.category_id " +
                "JOIN suppliers s ON b.supplier_id = s.supplier_id " +
                "WHERE p.is_active = TRUE " +
                "  AND b.current_quantity > 0 " +
                "  AND b.status != 'DEPLETED' " +
                "  AND b.expiry_date < CURRENT_DATE "
        );

        List<Object> params = new ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append("AND c.category_id = ? ");
            params.add(categoryId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.product_name) LIKE ? OR LOWER(b.batch_number) LIKE ? OR LOWER(p.sku) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("ORDER BY b.expiry_date ASC, b.current_quantity DESC");

        List<ExpiryAlert> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExpiryAlert(rs, ExpirySeverity.EXPIRED));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves unexpired batches expiring within maxDays (e.g. 7, 15, 30 days).
     */
    public List<ExpiryAlert> findExpiringSoonBatches(int maxDays, Integer categoryId, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT b.batch_id, b.batch_number, b.current_quantity, b.expiry_date, " +
                "p.product_id, p.product_name, p.sku, p.unit, " +
                "c.category_id, c.category_name, s.supplier_name, " +
                "DATEDIFF(b.expiry_date, CURRENT_DATE) AS days_remaining " +
                "FROM product_batches b " +
                "JOIN products p ON b.product_id = p.product_id " +
                "JOIN categories c ON p.category_id = c.category_id " +
                "JOIN suppliers s ON b.supplier_id = s.supplier_id " +
                "WHERE p.is_active = TRUE " +
                "  AND b.current_quantity > 0 " +
                "  AND b.status != 'DEPLETED' " +
                "  AND b.expiry_date >= CURRENT_DATE " +
                "  AND DATEDIFF(b.expiry_date, CURRENT_DATE) <= ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(maxDays);

        if (categoryId != null && categoryId > 0) {
            sql.append("AND c.category_id = ? ");
            params.add(categoryId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.product_name) LIKE ? OR LOWER(b.batch_number) LIKE ? OR LOWER(p.sku) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("ORDER BY b.expiry_date ASC, b.current_quantity DESC");

        List<ExpiryAlert> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long days = rs.getInt("days_remaining");
                    ExpirySeverity severity;
                    if (days <= 7) {
                        severity = ExpirySeverity.EXPIRING_7_DAYS;
                    } else if (days <= 15) {
                        severity = ExpirySeverity.EXPIRING_15_DAYS;
                    } else {
                        severity = ExpirySeverity.EXPIRING_30_DAYS;
                    }
                    list.add(mapResultSetToExpiryAlert(rs, severity));
                }
            }
        }
        return list;
    }

    /**
     * Detects low-stock and out-of-stock active products by summing eligible, unexpired batch quantities.
     */
    public List<LowStockAlert> findLowStockAlerts(Integer categoryId, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.product_id, p.product_name, p.sku, p.unit, p.min_stock_level, " +
                "c.category_id, c.category_name, " +
                "COALESCE(SUM(CASE " +
                "    WHEN b.current_quantity > 0 " +
                "     AND b.status IN ('ACTIVE', 'EXPIRING_SOON') " +
                "     AND b.expiry_date >= CURRENT_DATE " +
                "    THEN b.current_quantity " +
                "    ELSE 0 " +
                "END), 0) AS current_stock " +
                "FROM products p " +
                "JOIN categories c ON p.category_id = c.category_id " +
                "LEFT JOIN product_batches b ON p.product_id = b.product_id " +
                "WHERE p.is_active = TRUE "
        );

        List<Object> params = new ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append("AND c.category_id = ? ");
            params.add(categoryId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.product_name) LIKE ? OR LOWER(p.sku) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("GROUP BY p.product_id, p.product_name, p.sku, p.unit, p.min_stock_level, c.category_id, c.category_name ");
        sql.append("HAVING current_stock <= p.min_stock_level ");
        sql.append("ORDER BY (current_stock = 0) DESC, (p.min_stock_level - current_stock) DESC, p.product_name ASC");

        List<LowStockAlert> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new LowStockAlert(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("sku"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getString("unit"),
                            rs.getInt("min_stock_level"),
                            rs.getInt("current_stock")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Synchronizes batch status ENUM in database according to actual calendar dates.
     */
    public void synchronizeBatchStatuses() throws SQLException {
        String markExpiredSql =
                "UPDATE product_batches " +
                "SET status = 'EXPIRED' " +
                "WHERE expiry_date < CURRENT_DATE AND current_quantity > 0 AND status != 'EXPIRED'";

        String markExpiringSoonSql =
                "UPDATE product_batches " +
                "SET status = 'EXPIRING_SOON' " +
                "WHERE expiry_date >= CURRENT_DATE " +
                "  AND DATEDIFF(expiry_date, CURRENT_DATE) <= 30 " +
                "  AND current_quantity > 0 " +
                "  AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(markExpiredSql);
            stmt.executeUpdate(markExpiringSoonSql);
        }
    }

    private ExpiryAlert mapResultSetToExpiryAlert(ResultSet rs, ExpirySeverity severity) throws SQLException {
        Date expDate = rs.getDate("expiry_date");
        return new ExpiryAlert(
                rs.getInt("batch_id"),
                rs.getString("batch_number"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("sku"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getString("supplier_name"),
                rs.getInt("current_quantity"),
                rs.getString("unit"),
                expDate != null ? expDate.toLocalDate() : LocalDate.now(),
                (long) rs.getInt("days_remaining"),
                severity
        );
    }
}
