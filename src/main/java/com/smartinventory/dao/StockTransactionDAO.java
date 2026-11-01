package com.smartinventory.dao;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.StockTransaction;
import com.smartinventory.model.enums.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for stock_transactions table representing immutable movement logs.
 */
public class StockTransactionDAO {

    private static final String BASE_SELECT_SQL =
            "SELECT t.transaction_id, t.product_id, p.product_name, p.sku AS product_sku, " +
            "t.batch_id, b.batch_number, t.user_id, u.username, t.transaction_type, " +
            "t.quantity, t.balance_after, t.reference_note, t.created_at " +
            "FROM stock_transactions t " +
            "JOIN products p ON t.product_id = p.product_id " +
            "JOIN product_batches b ON t.batch_id = b.batch_id " +
            "JOIN users u ON t.user_id = u.user_id ";

    public StockTransaction record(StockTransaction tx) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return record(conn, tx);
        }
    }

    public StockTransaction record(Connection conn, StockTransaction tx) throws SQLException {
        String sql = "INSERT INTO stock_transactions (product_id, batch_id, user_id, " +
                     "transaction_type, quantity, balance_after, reference_note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, tx.getProductId());
            stmt.setInt(2, tx.getBatchId());
            stmt.setInt(3, tx.getUserId());
            stmt.setString(4, tx.getTransactionType().name());
            stmt.setInt(5, tx.getQuantity());
            stmt.setInt(6, tx.getBalanceAfter());
            stmt.setString(7, tx.getReferenceNote() != null ? tx.getReferenceNote().trim() : null);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Recording transaction failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    tx.setTransactionId(keys.getLong(1));
                } else {
                    throw new SQLException("Recording transaction failed, no ID obtained.");
                }
            }
        }
        return tx;
    }

    public List<StockTransaction> findAll(int limit) throws SQLException {
        String sql = BASE_SELECT_SQL + "ORDER BY t.created_at DESC, t.transaction_id DESC LIMIT ?";
        List<StockTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return list;
    }

    public List<StockTransaction> findByProductId(int productId) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE t.product_id = ? ORDER BY t.created_at DESC, t.transaction_id DESC";
        List<StockTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return list;
    }

    public List<StockTransaction> findByBatchId(int batchId) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE t.batch_id = ? ORDER BY t.created_at DESC, t.transaction_id DESC";
        List<StockTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return list;
    }

    public List<StockTransaction> search(String keyword, TransactionType type, Integer productId, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT_SQL).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.product_name) LIKE ? OR LOWER(b.batch_number) LIKE ? OR LOWER(t.reference_note) LIKE ? OR LOWER(u.username) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (type != null) {
            sql.append("AND t.transaction_type = ? ");
            params.add(type.name());
        }

        if (productId != null && productId > 0) {
            sql.append("AND t.product_id = ? ");
            params.add(productId);
        }

        sql.append("ORDER BY t.created_at DESC, t.transaction_id DESC LIMIT ?");
        params.add(Math.max(1, limit));

        List<StockTransaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return list;
    }

    private StockTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        return new StockTransaction(
                rs.getLong("transaction_id"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("product_sku"),
                rs.getInt("batch_id"),
                rs.getString("batch_number"),
                rs.getInt("user_id"),
                rs.getString("username"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getInt("quantity"),
                rs.getInt("balance_after"),
                rs.getString("reference_note"),
                rs.getTimestamp("created_at")
        );
    }
}
