package com.smartinventory.dao;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for products table with category joins and prepared statements.
 */
public class ProductDAO {

    private static final String BASE_SELECT_SQL =
            "SELECT p.product_id, p.category_id, c.category_name, p.sku, p.barcode, " +
            "p.product_name, p.description, p.unit, p.min_stock_level, p.is_active, " +
            "p.created_at, p.updated_at " +
            "FROM products p " +
            "JOIN categories c ON p.category_id = c.category_id ";

    public Product create(Product product) throws SQLException {
        String sql = "INSERT INTO products (category_id, sku, barcode, product_name, " +
                     "description, unit, min_stock_level, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, product.getCategoryId());
            stmt.setString(2, product.getSku().trim());

            if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
                stmt.setString(3, product.getBarcode().trim());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            stmt.setString(4, product.getProductName().trim());
            stmt.setString(5, product.getDescription() != null ? product.getDescription().trim() : null);
            stmt.setString(6, product.getUnit() != null ? product.getUnit().trim() : "pcs");
            stmt.setInt(7, product.getMinStockLevel());
            stmt.setBoolean(8, product.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setProductId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
        }
        return product;
    }

    public Optional<Product> findById(int productId) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE p.product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findBySku(String sku) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE LOWER(p.sku) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sku.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findByBarcode(String barcode) throws SQLException {
        if (barcode == null || barcode.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = BASE_SELECT_SQL + "WHERE p.barcode = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, barcode.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Product> findAll() throws SQLException {
        String sql = BASE_SELECT_SQL + "ORDER BY p.product_name ASC";
        List<Product> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        }
        return list;
    }

    public List<Product> findByCategoryId(int categoryId) throws SQLException {
        String sql = BASE_SELECT_SQL + "WHERE p.category_id = ? ORDER BY p.product_name ASC";
        List<Product> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        }
        return list;
    }

    public List<Product> search(String keyword, Integer categoryId, Boolean activeOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT_SQL).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.product_name) LIKE ? OR LOWER(p.sku) LIKE ? OR LOWER(p.barcode) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }

        if (activeOnly != null) {
            sql.append("AND p.is_active = ? ");
            params.add(activeOnly);
        }

        sql.append("ORDER BY p.product_name ASC");

        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        }
        return list;
    }

    public boolean update(Product product) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, sku = ?, barcode = ?, " +
                     "product_name = ?, description = ?, unit = ?, min_stock_level = ?, " +
                     "is_active = ? WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, product.getCategoryId());
            stmt.setString(2, product.getSku().trim());

            if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
                stmt.setString(3, product.getBarcode().trim());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            stmt.setString(4, product.getProductName().trim());
            stmt.setString(5, product.getDescription() != null ? product.getDescription().trim() : null);
            stmt.setString(6, product.getUnit() != null ? product.getUnit().trim() : "pcs");
            stmt.setInt(7, product.getMinStockLevel());
            stmt.setBoolean(8, product.isActive());
            stmt.setInt(9, product.getProductId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean existsBySku(String sku, Integer excludeId) throws SQLException {
        if (sku == null || sku.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM products WHERE LOWER(sku) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND product_id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sku.trim());
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByBarcode(String barcode, Integer excludeId) throws SQLException {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM products WHERE barcode = ?";
        if (excludeId != null) {
            sql += " AND product_id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, barcode.trim());
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countBatchesForProduct(int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_batches WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getString("sku"),
                rs.getString("barcode"),
                rs.getString("product_name"),
                rs.getString("description"),
                rs.getString("unit"),
                rs.getInt("min_stock_level"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}
