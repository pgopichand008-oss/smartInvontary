package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.dao.BatchDAO;
import com.smartinventory.dao.ProductDAO;
import com.smartinventory.dao.StockTransactionDAO;
import com.smartinventory.dao.SupplierDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.InsufficientStockException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Batch;
import com.smartinventory.model.Product;
import com.smartinventory.model.StockAllocation;
import com.smartinventory.model.StockOutResult;
import com.smartinventory.model.StockTransaction;
import com.smartinventory.model.Supplier;
import com.smartinventory.model.User;
import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.util.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service Layer enforcing atomic inventory operations, FEFO allocations,
 * and immutable stock movement recording.
 */
public class InventoryService {

    public interface StockChangeListener {
        void onStockChanged();
    }

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final BatchDAO batchDAO;
    private final StockTransactionDAO transactionDAO;
    private final ProductDAO productDAO;
    private final SupplierDAO supplierDAO;
    private final List<StockChangeListener> stockChangeListeners = new CopyOnWriteArrayList<>();

    public InventoryService() {
        this(new BatchDAO(), new StockTransactionDAO(), new ProductDAO(), new SupplierDAO());
    }

    public InventoryService(BatchDAO batchDAO, StockTransactionDAO transactionDAO,
                            ProductDAO productDAO, SupplierDAO supplierDAO) {
        this.batchDAO = batchDAO;
        this.transactionDAO = transactionDAO;
        this.productDAO = productDAO;
        this.supplierDAO = supplierDAO;
    }

    public void addStockChangeListener(StockChangeListener listener) {
        if (listener != null && !stockChangeListeners.contains(listener)) {
            stockChangeListeners.add(listener);
        }
    }

    public void removeStockChangeListener(StockChangeListener listener) {
        stockChangeListeners.remove(listener);
    }

    private void notifyStockChanged() {
        for (StockChangeListener listener : stockChangeListeners) {
            try {
                listener.onStockChanged();
            } catch (Exception e) {
                logger.warn("Error notifying stock change listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Executes an atomic Stock-In operation: creates a new batch lot and records
     * the incoming movement in stock_transactions.
     */
    public Batch stockIn(Batch batch, Integer operatorUserId, String referenceNote) {
        validateStockInBatch(batch);

        int userId = resolveUserId(operatorUserId);
        String note = (referenceNote != null && !referenceNote.trim().isEmpty())
                ? referenceNote.trim() : "Stock In - Batch reception (" + batch.getBatchNumber() + ")";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check for duplicate batch number under same product
                if (batchDAO.existsByBatchNumber(batch.getProductId(), batch.getBatchNumber(), null)) {
                    throw new DuplicateResourceException("Batch number '" + batch.getBatchNumber().trim() +
                            "' already exists for this product.");
                }

                // 1. Create product_batches row
                Batch createdBatch = batchDAO.create(conn, batch);

                // 2. Record immutable stock transaction
                StockTransaction tx = new StockTransaction(
                        createdBatch.getProductId(),
                        createdBatch.getBatchId(),
                        userId,
                        TransactionType.PURCHASE_IN,
                        createdBatch.getInitialQuantity(),
                        createdBatch.getCurrentQuantity(),
                        note
                );
                transactionDAO.record(conn, tx);

                conn.commit();
                notifyStockChanged();
                logger.info("Stock In successful for Product ID {} - Batch {} (Qty: {})",
                        createdBatch.getProductId(), createdBatch.getBatchNumber(), createdBatch.getInitialQuantity());
                return createdBatch;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ValidationException) {
                    throw (ValidationException) e;
                }
                logger.error("Database error during Stock In rollback: {}", e.getMessage(), e);
                throw new DatabaseException("Failed to complete Stock In operation: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure in Stock In: {}", e.getMessage(), e);
            throw new DatabaseException("Database error in Stock In: " + e.getMessage(), e);
        }
    }

    /**
     * Executes an atomic Stock-Out operation with FEFO (First Expiry, First Out) allocation.
     * Prevents stock-out if eligible stock is insufficient.
     */
    public StockOutResult stockOut(int productId, int quantity, TransactionType type,
                                   String referenceNote, Integer operatorUserId) {
        if (productId <= 0) {
            throw new ValidationException("Invalid product ID: " + productId);
        }
        if (quantity <= 0) {
            throw new ValidationException("Stock-out quantity must be a positive integer greater than zero.");
        }
        if (type == null || type.isIncoming()) {
            throw new ValidationException("Transaction type must be an outgoing movement (e.g., SALE_OUT, DAMAGE_OUT).");
        }

        Product product = getProductOrThrow(productId);
        int userId = resolveUserId(operatorUserId);
        String note = (referenceNote != null && !referenceNote.trim().isEmpty())
                ? referenceNote.trim() : (type.getDisplayName() + " dispatch");

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Fetch eligible unexpired batches ordered by FEFO (expiry_date ASC, batch_id ASC)
                List<Batch> eligibleBatches = batchDAO.findEligibleBatchesForFefo(conn, productId);

                // 2. Validate total eligible available stock
                int totalAvailable = eligibleBatches.stream().mapToInt(Batch::getCurrentQuantity).sum();
                if (totalAvailable < quantity) {
                    throw new InsufficientStockException(product.getProductName(), quantity, totalAvailable);
                }

                // 3. Allocate and deduct across batches in FEFO sequence
                int remainingToDeduct = quantity;
                List<StockAllocation> allocations = new ArrayList<>();

                for (Batch batch : eligibleBatches) {
                    if (remainingToDeduct <= 0) {
                        break;
                    }

                    int deduct = Math.min(batch.getCurrentQuantity(), remainingToDeduct);
                    int newBalance = batch.getCurrentQuantity() - deduct;
                    BatchStatus newStatus = (newBalance == 0) ? BatchStatus.DEPLETED : batch.getStatus();

                    // Update batch current quantity & status
                    batchDAO.updateQuantityAndStatus(conn, batch.getBatchId(), newBalance, newStatus);

                    // Record immutable transaction entry
                    StockTransaction tx = new StockTransaction(
                            productId,
                            batch.getBatchId(),
                            userId,
                            type,
                            deduct,
                            newBalance,
                            note
                    );
                    transactionDAO.record(conn, tx);

                    allocations.add(new StockAllocation(
                            batch.getBatchId(),
                            batch.getBatchNumber(),
                            batch.getExpiryDate(),
                            deduct,
                            newBalance,
                            batch.getSellingPrice(),
                            batch.getPurchasePrice()
                    ));

                    remainingToDeduct -= deduct;
                }

                // 4. Calculate remaining total stock for product
                int remainingTotalStock = batchDAO.getTotalStockForProduct(conn, productId);

                conn.commit();
                notifyStockChanged();
                logger.info("Stock Out (FEFO) completed for Product '{}': Deducted {} across {} batch(es). Remaining: {}",
                        product.getProductName(), quantity, allocations.size(), remainingTotalStock);

                return new StockOutResult(productId, product.getProductName(), quantity, quantity,
                        remainingTotalStock, allocations);
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ValidationException) {
                    throw (ValidationException) e;
                }
                logger.error("Database error during Stock Out rollback: {}", e.getMessage(), e);
                throw new DatabaseException("Failed to complete Stock Out operation: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure in Stock Out: {}", e.getMessage(), e);
            throw new DatabaseException("Database error in Stock Out: " + e.getMessage(), e);
        }
    }

    public List<Batch> getAllBatches() {
        try {
            return batchDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all batches: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to fetch batches: " + e.getMessage(), e);
        }
    }

    public List<Batch> getBatchesByProduct(int productId) {
        if (productId <= 0) {
            throw new ValidationException("Invalid product ID: " + productId);
        }
        try {
            return batchDAO.findByProductId(productId);
        } catch (SQLException e) {
            logger.error("Error fetching batches for product ID {}: {}", productId, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch product batches: " + e.getMessage(), e);
        }
    }

    public Optional<Batch> getBatchById(int batchId) {
        if (batchId <= 0) {
            return Optional.empty();
        }
        try {
            return batchDAO.findById(batchId);
        } catch (SQLException e) {
            logger.error("Error fetching batch ID {}: {}", batchId, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch batch: " + e.getMessage(), e);
        }
    }

    public int getTotalStockForProduct(int productId) {
        if (productId <= 0) {
            return 0;
        }
        try {
            return batchDAO.getTotalStockForProduct(productId);
        } catch (SQLException e) {
            logger.error("Error fetching total stock for product {}: {}", productId, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch total stock: " + e.getMessage(), e);
        }
    }

    public List<Batch> searchBatches(String keyword, Integer productId, Integer supplierId, BatchStatus status) {
        try {
            return batchDAO.search(keyword, productId, supplierId, status);
        } catch (SQLException e) {
            logger.error("Error searching batches: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to search batches: " + e.getMessage(), e);
        }
    }

    public List<StockTransaction> getAllTransactions(int limit) {
        try {
            return transactionDAO.findAll(limit);
        } catch (SQLException e) {
            logger.error("Error fetching transactions: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to fetch transactions: " + e.getMessage(), e);
        }
    }

    public List<StockTransaction> searchTransactions(String keyword, TransactionType type, Integer productId, int limit) {
        try {
            return transactionDAO.search(keyword, type, productId, limit);
        } catch (SQLException e) {
            logger.error("Error searching transactions: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to search transactions: " + e.getMessage(), e);
        }
    }

    private void validateStockInBatch(Batch batch) {
        if (batch == null) {
            throw new ValidationException("Batch cannot be null.");
        }

        // Product validation
        if (batch.getProductId() == null || batch.getProductId() <= 0) {
            throw new ValidationException("A valid product must be selected for Stock In.");
        }
        try {
            Optional<Product> prodOpt = productDAO.findById(batch.getProductId());
            if (!prodOpt.isPresent()) {
                throw new ValidationException("Selected product does not exist (ID: " + batch.getProductId() + ").");
            }
            if (!prodOpt.get().isActive()) {
                throw new ValidationException("Cannot stock in to an inactive product catalog item.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify product: " + e.getMessage(), e);
        }

        // Supplier validation
        if (batch.getSupplierId() == null || batch.getSupplierId() <= 0) {
            throw new ValidationException("A valid supplier must be selected for Stock In.");
        }
        try {
            Optional<Supplier> suppOpt = supplierDAO.findById(batch.getSupplierId());
            if (!suppOpt.isPresent()) {
                throw new ValidationException("Selected supplier does not exist (ID: " + batch.getSupplierId() + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify supplier: " + e.getMessage(), e);
        }

        // Batch number validation
        if (batch.getBatchNumber() == null || batch.getBatchNumber().trim().isEmpty()) {
            throw new ValidationException("Batch number is required.");
        }
        if (batch.getBatchNumber().trim().length() < 2 || batch.getBatchNumber().trim().length() > 50) {
            throw new ValidationException("Batch number must be between 2 and 50 characters.");
        }

        // Pricing validations
        if (batch.getPurchasePrice() == null || batch.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Purchase price must be greater than or equal to zero.");
        }
        if (batch.getSellingPrice() == null || batch.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Selling price must be greater than or equal to zero.");
        }
        if (batch.getSellingPrice().compareTo(batch.getPurchasePrice()) < 0) {
            throw new ValidationException("Selling price (" + batch.getSellingPrice() +
                    ") cannot be less than purchase price (" + batch.getPurchasePrice() + ").");
        }

        // Quantity validation
        if (batch.getInitialQuantity() <= 0) {
            throw new ValidationException("Initial quantity must be a positive integer greater than zero.");
        }
        batch.setCurrentQuantity(batch.getInitialQuantity());

        // Date validations
        if (batch.getExpiryDate() == null) {
            throw new ValidationException("Expiry date is required.");
        }
        if (batch.getManufacturingDate() != null) {
            if (batch.getManufacturingDate().isAfter(batch.getExpiryDate())) {
                throw new ValidationException("Manufacturing date (" + batch.getManufacturingDate() +
                        ") cannot be after expiry date (" + batch.getExpiryDate() + ").");
            }
            if (batch.getManufacturingDate().isAfter(LocalDate.now())) {
                throw new ValidationException("Manufacturing date cannot be in the future.");
            }
        }
    }

    private Product getProductOrThrow(int productId) {
        try {
            Optional<Product> prodOpt = productDAO.findById(productId);
            if (!prodOpt.isPresent()) {
                throw new ValidationException("Product not found with ID: " + productId);
            }
            return prodOpt.get();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify product: " + e.getMessage(), e);
        }
    }

    private int resolveUserId(Integer explicitUserId) {
        if (explicitUserId != null && explicitUserId > 0) {
            return explicitUserId;
        }
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUserId() != null) {
            return currentUser.getUserId();
        }
        return 1; // Default Admin fallback
    }
}
