package com.smartinventory.model;

import java.util.Collections;
import java.util.List;

/**
 * Result summary of a FEFO Stock-Out operation detailing allocated batches.
 */
public class StockOutResult {

    private final int productId;
    private final String productName;
    private final int totalRequested;
    private final int totalDeducted;
    private final int remainingProductStock;
    private final List<StockAllocation> allocations;

    public StockOutResult(int productId, String productName, int totalRequested,
                          int totalDeducted, int remainingProductStock,
                          List<StockAllocation> allocations) {
        this.productId = productId;
        this.productName = productName;
        this.totalRequested = totalRequested;
        this.totalDeducted = totalDeducted;
        this.remainingProductStock = remainingProductStock;
        this.allocations = allocations != null ? allocations : Collections.emptyList();
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public int getTotalDeducted() {
        return totalDeducted;
    }

    public int getRemainingProductStock() {
        return remainingProductStock;
    }

    public List<StockAllocation> getAllocations() {
        return allocations;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Successfully allocated ").append(totalDeducted).append(" unit(s) of ").append(productName).append(" across ").append(allocations.size()).append(" batch(es):\n");
        for (StockAllocation alloc : allocations) {
            sb.append(" • ").append(alloc).append("\n");
        }
        sb.append("Remaining total stock: ").append(remainingProductStock).append(" unit(s).");
        return sb.toString();
    }
}
