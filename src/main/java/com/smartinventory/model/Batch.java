package com.smartinventory.model;

import com.smartinventory.model.enums.BatchStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain entity representing an inventory batch lot with quantity, pricing, and expiration dates.
 */
public class Batch {

    private Integer batchId;
    private Integer productId;
    private String productName;   // Convenience joined field
    private String productSku;    // Convenience joined field
    private String productUnit;   // Convenience joined field
    private Integer categoryId;   // Convenience joined field
    private String categoryName;  // Convenience joined field
    private Integer supplierId;
    private String supplierName;  // Convenience joined field
    private String batchNumber;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int initialQuantity;
    private int currentQuantity;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private BatchStatus status = BatchStatus.ACTIVE;
    private LocalDate receivedDate = LocalDate.now();
    private Timestamp createdAt;

    public Batch() {
    }

    public Batch(Integer productId, Integer supplierId, String batchNumber,
                 BigDecimal purchasePrice, BigDecimal sellingPrice,
                 int initialQuantity, int currentQuantity,
                 LocalDate manufacturingDate, LocalDate expiryDate) {
        this.productId = productId;
        this.supplierId = supplierId;
        this.batchNumber = batchNumber;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.initialQuantity = initialQuantity;
        this.currentQuantity = currentQuantity;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
        this.status = (currentQuantity == 0) ? BatchStatus.DEPLETED : BatchStatus.ACTIVE;
        this.receivedDate = LocalDate.now();
    }

    public Batch(Integer batchId, Integer productId, String productName, String productSku,
                 String productUnit, Integer categoryId, String categoryName,
                 Integer supplierId, String supplierName,
                 String batchNumber, BigDecimal purchasePrice, BigDecimal sellingPrice,
                 int initialQuantity, int currentQuantity, LocalDate manufacturingDate,
                 LocalDate expiryDate, BatchStatus status, LocalDate receivedDate,
                 Timestamp createdAt) {
        this.batchId = batchId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.productUnit = productUnit;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.batchNumber = batchNumber;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.initialQuantity = initialQuantity;
        this.currentQuantity = currentQuantity;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.receivedDate = receivedDate;
        this.createdAt = createdAt;
    }

    public Batch(Integer batchId, Integer productId, String productName, String productSku,
                 String productUnit, Integer supplierId, String supplierName,
                 String batchNumber, BigDecimal purchasePrice, BigDecimal sellingPrice,
                 int initialQuantity, int currentQuantity, LocalDate manufacturingDate,
                 LocalDate expiryDate, BatchStatus status, LocalDate receivedDate,
                 Timestamp createdAt) {
        this(batchId, productId, productName, productSku, productUnit, null, null, supplierId, supplierName,
             batchNumber, purchasePrice, sellingPrice, initialQuantity, currentQuantity, manufacturingDate,
             expiryDate, status, receivedDate, createdAt);
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(String productUnit) {
        this.productUnit = productUnit;
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

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(int initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Batch batch = (Batch) o;
        return Objects.equals(batchId, batch.batchId) &&
                Objects.equals(productId, batch.productId) &&
                Objects.equals(batchNumber, batch.batchNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, productId, batchNumber);
    }

    @Override
    public String toString() {
        return batchNumber + " (" + currentQuantity + " remaining, exp: " + expiryDate + ")";
    }
}
