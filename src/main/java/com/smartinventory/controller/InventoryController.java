package com.smartinventory.controller;

import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Batch;
import com.smartinventory.model.StockOutResult;
import com.smartinventory.model.StockTransaction;
import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.service.InventoryService;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller coordinating Inventory & Batch views with asynchronous SwingWorker operations.
 */
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController() {
        this(new InventoryService());
    }

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    /**
     * Asynchronously loads batches matching filter criteria.
     */
    public void loadBatches(Integer productId, Integer supplierId, BatchStatus status,
                            Consumer<List<Batch>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Batch>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Batch> doInBackground() {
                return inventoryService.searchBatches(null, productId, supplierId, status);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously searches batches.
     */
    public void searchBatches(String keyword, Integer productId, Integer supplierId, BatchStatus status,
                              Consumer<List<Batch>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Batch>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Batch> doInBackground() {
                return inventoryService.searchBatches(keyword, productId, supplierId, status);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously loads transaction ledger records.
     */
    public void loadTransactions(int limit, Consumer<List<StockTransaction>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<StockTransaction>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<StockTransaction> doInBackground() {
                return inventoryService.getAllTransactions(limit);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously searches stock transactions.
     */
    public void searchTransactions(String keyword, TransactionType type, Integer productId, int limit,
                                   Consumer<List<StockTransaction>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<StockTransaction>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<StockTransaction> doInBackground() {
                return inventoryService.searchTransactions(keyword, type, productId, limit);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Executes atomic Stock In asynchronously.
     */
    public void handleStockIn(Batch batch, Integer operatorUserId, String referenceNote,
                              Consumer<Batch> onSuccess, Consumer<String> onError) {
        SwingWorker<Batch, Void> worker = new SwingWorker<>() {
            @Override
            protected Batch doInBackground() {
                return inventoryService.stockIn(batch, operatorUserId, referenceNote);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Executes atomic Stock Out with FEFO allocation asynchronously.
     */
    public void handleStockOut(int productId, int quantity, TransactionType type,
                               String referenceNote, Integer operatorUserId,
                               Consumer<StockOutResult> onSuccess, Consumer<String> onError) {
        SwingWorker<StockOutResult, Void> worker = new SwingWorker<>() {
            @Override
            protected StockOutResult doInBackground() {
                return inventoryService.stockOut(productId, quantity, type, referenceNote, operatorUserId);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously retrieves total available stock for a product.
     */
    public void loadProductTotalStock(int productId, Consumer<Integer> onSuccess, Consumer<String> onError) {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return inventoryService.getTotalStockForProduct(productId);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
