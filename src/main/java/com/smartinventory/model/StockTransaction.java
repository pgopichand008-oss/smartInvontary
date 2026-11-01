package com.smartinventory.model;

import com.smartinventory.model.enums.TransactionType;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing an immutable ledger record of a stock movement.
 */
public class StockTransaction {

    private Long transactionId;
    private Integer productId;
    private String productName;
    private String productSku;
    private Integer batchId;
    private String batchNumber;
    private Integer userId;
    private String username;
    private TransactionType transactionType;
    private int quantity;
    private int balanceAfter;
    private String referenceNote;
    private Timestamp createdAt;

    public StockTransaction() {
    }

    public StockTransaction(Integer productId, Integer batchId, Integer userId,
                            TransactionType transactionType, int quantity,
                            int balanceAfter, String referenceNote) {
        this.productId = productId;
        this.batchId = batchId;
        this.userId = userId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.referenceNote = referenceNote;
    }

    public StockTransaction(Long transactionId, Integer productId, String productName,
                            String productSku, Integer batchId, String batchNumber,
                            Integer userId, String username, TransactionType transactionType,
                            int quantity, int balanceAfter, String referenceNote,
                            Timestamp createdAt) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.userId = userId;
        this.username = username;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.referenceNote = referenceNote;
        this.createdAt = createdAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
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

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(int balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceNote() {
        return referenceNote;
    }

    public void setReferenceNote(String referenceNote) {
        this.referenceNote = referenceNote;
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
        StockTransaction that = (StockTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}
