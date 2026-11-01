package com.smartinventory.dao;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.Batch;
import com.smartinventory.model.enums.BatchStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for product_batches table supporting FEFO locking queries and atomic updates.
 */
public class BatchDAO {

    private static final String BASE_SELECT_SQL =
            "SELECT b.batch_id, b.product_id, p.product_name, p.sku AS product_sku, p.unit AS product_unit, " +
            "b.supplier_id, s.supplier_name, b.batch_number, b.purchase_price, b.selling_price, " +
            "b.initial_quantity, b.current_quantity, b.manufacturing_date, b.expiry_date, " +
            "b.status, b.received_date, b.created_at " +
            "FROM product_batches b " +
            "JOIN products p ON b.product_id = p.product_id " +
            "JOIN suppliers s ON b.supplier_id = s.supplier_id ";

    public Batch create(Batch batch) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return create(conn, batch);
        }
    }

    public Batch create(Connection conn, Batch batch) throws SQLException {
        String sql = "INSERT INTO product_batches (product_id, supplier_id, batch_number, " +
                     "purchase_price, selling_price, initial_quantity, current_quantity, " +
                     "manufacturing_date, expiry_date, status, received_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, batch.getProductId());
            stmt.setInt(2, batch.getSupplierId());
            stmt.setString(3, batch.getBatchNumber().trim());
            stmt.setBigDecimal(4, batch.getPurchasePrice());
            stmt.setBigDecimal(5, batch.getSellingPrice());
            stmt.setInt(6, batch.getInitialQuantity());
            stmt.setInt(7, batch.getCurrentQuantity());

            if (batch.getManufacturingDate() != null) {
                stmt.setDate(8, Date.valueOf(batch.getManufacturingDate()));
            } else {
                stmt.setNull(8, Types.DATE);
            }

            stmt.setDate(9, Date.valueOf(batch.getExpiryDate()));
            stmt.setString(10, batch.getStatus() != null ? batch.getStatus().name() : BatchStatus.ACTIVE.name());
            stmt.setDate(11, batch.getReceivedDate() != null ? Date.valueOf(batch.getReceivedDate()) : new Date(System.currentTimeMillis()));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating batch failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    batch.setBatchId(keys.getInt(1));
                } else {
                    throw new SQLException("Creating batch failed, no ID obtained.");
                }
            }
        }
        return batch;
    }

    public Optional<Batch> findById(int batchId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, batchId);
        }
    }

    public Optional<Batch> findById(Connection conn, int batchId) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE b.batch_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, batchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBatch(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Batch> findByProductAndBatchNumber(int productId, String batchNumber) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE b.product_id = ? AND LOWER(b.batch_number) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setString(2, batchNumber.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBatch(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Batch> findAll() throws SQLException {
        String sql = BASE_SELECT_SQL + "ORDER BY b.expiry_date ASC, b.batch_id ASC";
        List<Batch> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToBatch(rs));
            }
        }
        return list;
    }

    public List<Batch> findByProductId(int productId) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE b.product_id = ? ORDER BY b.expiry_date ASC, b.batch_id ASC";
        List<Batch> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBatch(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all eligible unexpired batches for a product ordered by FEFO (earliest expiry first).
     * Locks rows with FOR UPDATE to prevent race conditions during allocation.
     */
    public List<Batch> findEligibleBatchesForFefo(Connection conn, int productId) throws SQLException {
        String sql = BASE_SELECT_SQL +
                     "WHERE b.product_id = ? " +
                     "  AND b.current_quantity > 0 " +
                     "  AND b.status IN ('ACTIVE', 'EXPIRING_SOON') " +
                     "  AND b.expiry_date >= CURRENT_DATE " +
                     "ORDER BY b.expiry_date ASC, b.batch_id ASC " +
                     "FOR UPDATE";

        List<Batch> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBatch(rs));
                }
            }
        }
        return list;
    }

    public boolean updateQuantityAndStatus(Connection conn, int batchId, int newQuantity, BatchStatus newStatus) throws SQLException {
        String sql = "UPDATE product_batches SET current_quantity = ?, status = ? WHERE batch_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newQuantity);
            stmt.setString(2, newStatus.name());
            stmt.setInt(3, batchId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int getTotalStockForProduct(int productId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getTotalStockForProduct(conn, productId);
        }
    }

    public int getTotalStockForProduct(Connection conn, int productId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(current_quantity), 0) FROM product_batches " +
                     "WHERE product_id = ? AND status IN ('ACTIVE', 'EXPIRING_SOON') AND expiry_date >= CURRENT_DATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean existsByBatchNumber(int productId, String batchNumber, Integer excludeBatchId) throws SQLException {
        String sql = "SELECT 1 FROM product_batches WHERE product_id = ? AND LOWER(batch_number) = LOWER(?)";
        if (excludeBatchId != null) {
            sql += " AND batch_id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setString(2, batchNumber.trim());
            if (excludeBatchId != null) {
                stmt.setInt(3, excludeBatchId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Batch> search(String keyword, Integer productId, Integer supplierId, BatchStatus status) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT_SQL).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(b.batch_number) LIKE ? OR LOWER(p.product_name) LIKE ? OR LOWER(p.sku) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (productId != null && productId > 0) {
            sql.append("AND b.product_id = ? ");
            params.add(productId);
        }

        if (supplierId != null && supplierId > 0) {
            sql.append("AND b.supplier_id = ? ");
            params.add(supplierId);
        }

        if (status != null) {
            sql.append("AND b.status = ? ");
            params.add(status.name());
        }

        sql.append("ORDER BY b.expiry_date ASC, b.batch_id ASC");

        List<Batch> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBatch(rs));
                }
            }
        }
        return list;
    }

    private Batch mapResultSetToBatch(ResultSet rs) throws SQLException {
        Date mfgDate = rs.getDate("manufacturing_date");
        Date expDate = rs.getDate("expiry_date");
        Date recDate = rs.getDate("received_date");

        return new Batch(
                rs.getInt("batch_id"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("product_sku"),
                rs.getString("product_unit"),
                rs.getInt("supplier_id"),
                rs.getString("supplier_name"),
                rs.getString("batch_number"),
                rs.getBigDecimal("purchase_price"),
                rs.getBigDecimal("selling_price"),
                rs.getInt("initial_quantity"),
                rs.getInt("current_quantity"),
                mfgDate != null ? mfgDate.toLocalDate() : null,
                expDate != null ? expDate.toLocalDate() : null,
                BatchStatus.valueOf(rs.getString("status")),
                recDate != null ? recDate.toLocalDate() : null,
                rs.getTimestamp("created_at")
        );
    }
}
