package com.smartinventory.dao;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for suppliers table.
 * Strictly uses PreparedStatement for all SQL queries.
 */
public class SupplierDAO {

    public Supplier create(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers (supplier_name, contact_person, phone, email, address, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, supplier.getSupplierName().trim());
            stmt.setString(2, supplier.getContactPerson() != null ? supplier.getContactPerson().trim() : null);
            stmt.setString(3, supplier.getPhone().trim());
            stmt.setString(4, supplier.getEmail() != null ? supplier.getEmail().trim() : null);
            stmt.setString(5, supplier.getAddress() != null ? supplier.getAddress().trim() : null);
            stmt.setBoolean(6, supplier.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating supplier failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    supplier.setSupplierId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating supplier failed, no ID obtained.");
                }
            }
        }
        return supplier;
    }

    public Optional<Supplier> findById(int supplierId) throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, contact_person, phone, email, address, is_active, created_at " +
                "FROM suppliers WHERE supplier_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, supplierId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSupplier(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Supplier> findByName(String supplierName) throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, contact_person, phone, email, address, is_active, created_at " +
                "FROM suppliers WHERE LOWER(supplier_name) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, supplierName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSupplier(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Supplier> findAll() throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, contact_person, phone, email, address, is_active, created_at " +
                "FROM suppliers ORDER BY supplier_name ASC";
        List<Supplier> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToSupplier(rs));
            }
        }
        return list;
    }

    public List<Supplier> findActive() throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, contact_person, phone, email, address, is_active, created_at " +
                "FROM suppliers WHERE is_active = TRUE ORDER BY supplier_name ASC";
        List<Supplier> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToSupplier(rs));
            }
        }
        return list;
    }

    public List<Supplier> search(String keyword, Boolean activeOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT supplier_id, supplier_name, contact_person, phone, email, address, is_active, created_at " +
                        "FROM suppliers WHERE (LOWER(supplier_name) LIKE ? OR LOWER(contact_person) LIKE ? " +
                        "OR phone LIKE ? OR LOWER(email) LIKE ?) "
        );
        if (activeOnly != null) {
            sql.append("AND is_active = ? ");
        }
        sql.append("ORDER BY supplier_name ASC");

        List<Supplier> list = new ArrayList<>();
        String pattern = "%" + keyword.toLowerCase().trim() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);

            if (activeOnly != null) {
                stmt.setBoolean(5, activeOnly);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSupplier(rs));
                }
            }
        }
        return list;
    }

    public boolean update(Supplier supplier) throws SQLException {
        String sql = "UPDATE suppliers SET supplier_name = ?, contact_person = ?, phone = ?, " +
                "email = ?, address = ?, is_active = ? WHERE supplier_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, supplier.getSupplierName().trim());
            stmt.setString(2, supplier.getContactPerson() != null ? supplier.getContactPerson().trim() : null);
            stmt.setString(3, supplier.getPhone().trim());
            stmt.setString(4, supplier.getEmail() != null ? supplier.getEmail().trim() : null);
            stmt.setString(5, supplier.getAddress() != null ? supplier.getAddress().trim() : null);
            stmt.setBoolean(6, supplier.isActive());
            stmt.setInt(7, supplier.getSupplierId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int supplierId) throws SQLException {
        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, supplierId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean toggleStatus(int supplierId, boolean active) throws SQLException {
        String sql = "UPDATE suppliers SET is_active = ? WHERE supplier_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, active);
            stmt.setInt(2, supplierId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean existsByName(String name, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM suppliers WHERE LOWER(supplier_name) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND supplier_id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByPhone(String phone, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM suppliers WHERE phone = ?";
        if (excludeId != null) {
            sql += " AND supplier_id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone.trim());
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countBatchesFromSupplier(int supplierId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_batches WHERE supplier_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, supplierId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Supplier mapResultSetToSupplier(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getInt("supplier_id"),
                rs.getString("supplier_name"),
                rs.getString("contact_person"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at")
        );
    }
}
