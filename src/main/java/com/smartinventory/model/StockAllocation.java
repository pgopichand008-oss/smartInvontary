package com.smartinventory.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object describing an allocation from a specific batch during FEFO stock-out operations.
 */
public class StockAllocation {

    private final int batchId;
    private final String batchNumber;
    private final LocalDate expiryDate;
    private final int allocatedQuantity;
    private final int remainingBatchQuantity;
    private final BigDecimal sellingPrice;
    private final BigDecimal purchasePrice;

    public StockAllocation(int batchId, String batchNumber, LocalDate expiryDate,
                           int allocatedQuantity, int remainingBatchQuantity,
                           BigDecimal sellingPrice, BigDecimal purchasePrice) {
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.allocatedQuantity = allocatedQuantity;
        this.remainingBatchQuantity = remainingBatchQuantity;
        this.sellingPrice = sellingPrice;
        this.purchasePrice = purchasePrice;
    }

    public int getBatchId() {
        return batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public int getRemainingBatchQuantity() {
        return remainingBatchQuantity;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    @Override
    public String toString() {
        return String.format("Batch %s (Exp: %s): %d units deducted (%d left)",
                batchNumber, expiryDate, allocatedQuantity, remainingBatchQuantity);
    }
}
