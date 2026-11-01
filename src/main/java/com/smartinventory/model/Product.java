package com.smartinventory.model;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing a master product item.
 */
public class Product {

    private Integer productId;
    private Integer categoryId;
    private String categoryName; // Joined convenience field from categories table
    private String sku;
    private String barcode;
    private String productName;
    private String description;
    private String unit = "pcs";
    private int minStockLevel = 10;
    private boolean active = true;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Product() {
    }

    public Product(Integer categoryId, String sku, String barcode, String productName, 
                   String description, String unit, int minStockLevel) {
        this.categoryId = categoryId;
        this.sku = sku;
        this.barcode = barcode;
        this.productName = productName;
        this.description = description;
        this.unit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : "pcs";
        this.minStockLevel = minStockLevel;
        this.active = true;
    }

    public Product(Integer categoryId, String sku, String barcode, String productName, 
                   String description, String unit, int minStockLevel, boolean active) {
        this(categoryId, sku, barcode, productName, description, unit, minStockLevel);
        this.active = active;
    }

    public Product(Integer productId, Integer categoryId, String sku, String barcode, 
                   String productName, String description, String unit, int minStockLevel, boolean active) {
        this(categoryId, sku, barcode, productName, description, unit, minStockLevel, active);
        this.productId = productId;
    }

    public Product(Integer productId, Integer categoryId, String categoryName, String sku, 
                   String barcode, String productName, String description, String unit, 
                   int minStockLevel, boolean active, Timestamp createdAt, Timestamp updatedAt) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.sku = sku;
        this.barcode = barcode;
        this.productName = productName;
        this.description = description;
        this.unit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : "pcs";
        this.minStockLevel = minStockLevel;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(productId, product.productId) &&
                Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, sku);
    }

    @Override
    public String toString() {
        return productName != null ? productName : (sku != null ? sku : "Product #" + productId);
    }
}
