package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.InsufficientStockException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Batch;
import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import com.smartinventory.model.StockAllocation;
import com.smartinventory.model.StockOutResult;
import com.smartinventory.model.StockTransaction;
import com.smartinventory.model.Supplier;
import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.TransactionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for Phase 5:
 * InventoryService, BatchDAO, StockTransactionDAO, atomic transactions,
 * and FEFO (First Expiry, First Out) stock-out allocation logic.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryServiceTest {

    private static InventoryService inventoryService;
    private static ProductService productService;
    private static CategoryService categoryService;
    private static SupplierService supplierService;

    private static int testProductId;
    private static int testSupplierId;

    @BeforeAll
    public static void setUp() {
        inventoryService = new InventoryService();
        productService = new ProductService();
        categoryService = new CategoryService();
        supplierService = new SupplierService();

        // 1. Ensure a valid category exists
        List<Category> categories = categoryService.getAllCategories();
        int catId;
        if (categories.isEmpty()) {
            Category cat = categoryService.createCategory("Inv Category " + System.currentTimeMillis(), "Category for Inventory Tests");
            catId = cat.getCategoryId();
        } else {
            catId = categories.get(0).getCategoryId();
        }

        // 2. Create dedicated Product for testing
        long suffix = System.currentTimeMillis();
        Product product = productService.createProduct(
                catId,
                "INV-SKU-" + (suffix % 1000000),
                "INV-BAR-" + (suffix % 1000000),
                "Inventory Test Soda " + suffix,
                "Test beverage for multi-batch tracking",
                "can",
                10
        );
        testProductId = product.getProductId();

        // 3. Create dedicated Supplier for testing
        Supplier supplier = supplierService.createSupplier(
                "Inv Supplier " + suffix,
                "Bob Manager",
                "+1-800-" + (suffix % 10000),
                "inv" + suffix + "@supplier.com",
                "456 Supply Way"
        );
        testSupplierId = supplier.getSupplierId();
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    @Test
    @Order(1)
    @DisplayName("1. Stock In: Create physical batch lot and verify transaction ledger record")
    public void testStockInOperation() {
        long suffix = System.currentTimeMillis();
        String batchNum = "BAT-A-" + (suffix % 1000000);

        Batch batch = new Batch(
                testProductId,
                testSupplierId,
                batchNum,
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(15.00),
                100,
                100,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(180) // Expires in 180 days
        );

        Batch created = inventoryService.stockIn(batch, 1, "Initial shipment PO-101");

        assertNotNull(created.getBatchId(), "Batch ID must be generated");
        assertEquals(batchNum, created.getBatchNumber());
        assertEquals(100, created.getCurrentQuantity());
        assertEquals(BatchStatus.ACTIVE, created.getStatus());

        // Verify product total stock updated
        int totalStock = inventoryService.getTotalStockForProduct(testProductId);
        assertTrue(totalStock >= 100, "Total product stock must reflect received batch");

        // Verify stock_transactions recorded incoming entry
        List<StockTransaction> txs = inventoryService.getAllTransactions(50);
        Optional<StockTransaction> inTx = txs.stream()
                .filter(t -> t.getBatchId().equals(created.getBatchId()) && t.getTransactionType() == TransactionType.PURCHASE_IN)
                .findFirst();

        assertTrue(inTx.isPresent(), "PURCHASE_IN transaction must be recorded in ledger");
        assertEquals(100, inTx.get().getQuantity());
        assertEquals(100, inTx.get().getBalanceAfter());
    }

    @Test
    @Order(2)
    @DisplayName("2. Multi-Batch Support: Add second batch with earlier expiry date")
    public void testSupportMultipleBatchesForOneProduct() {
        long suffix = System.currentTimeMillis();
        String batchNum = "BAT-B-" + (suffix % 1000000);

        // Batch B has 50 units and expires SOONER (+90 days) than Batch A (+180 days)
        Batch batchB = new Batch(
                testProductId,
                testSupplierId,
                batchNum,
                BigDecimal.valueOf(11.00),
                BigDecimal.valueOf(16.50),
                50,
                50,
                LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(90) // Earlier expiry
        );

        Batch createdB = inventoryService.stockIn(batchB, 1, "Second shipment PO-102");
        assertNotNull(createdB.getBatchId());

        List<Batch> productBatches = inventoryService.getBatchesByProduct(testProductId);
        assertTrue(productBatches.size() >= 2, "Product should now have at least 2 active batches");
    }

    @Test
    @Order(3)
    @DisplayName("3. FEFO Allocation: Outgoing dispatch consumes earliest expiring batch first")
    public void testFefoAllocationEarliestExpiryFirst() {
        // Current state for testProductId:
        // Batch B (expires in 90 days): 50 units
        // Batch A (expires in 180 days): 100 units
        // Stock out 30 units: FEFO MUST allocate exclusively from Batch B!

        StockOutResult result = inventoryService.stockOut(
                testProductId,
                30,
                TransactionType.SALE_OUT,
                "Sales Order #501",
                1
        );

        assertNotNull(result);
        assertEquals(30, result.getTotalDeducted());
        assertEquals(1, result.getAllocations().size(), "Should only deduct from earliest expiring batch");

        StockAllocation alloc = result.getAllocations().get(0);
        assertTrue(alloc.getBatchNumber().startsWith("BAT-B-"), "FEFO must select Batch B (earliest expiry)");
        assertEquals(30, alloc.getAllocatedQuantity());
        assertEquals(20, alloc.getRemainingBatchQuantity(), "Batch B should have 20 units remaining");

        // Verify in DB
        Optional<Batch> batchB = inventoryService.getBatchById(alloc.getBatchId());
        assertTrue(batchB.isPresent());
        assertEquals(20, batchB.get().getCurrentQuantity());
        assertEquals(BatchStatus.ACTIVE, batchB.get().getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("4. Multi-Batch FEFO Allocation: Order spanning across multiple batches with depletion")
    public void testMultiBatchFefoAllocationWithDepletion() {
        // Current state:
        // Batch B (expires in 90 days): 20 units remaining
        // Batch A (expires in 180 days): 100 units remaining
        // Stock out 45 units:
        // - 20 units from Batch B (depleting it to 0 and setting status DEPLETED)
        // - 25 units from Batch A (reducing it to 75 units)

        StockOutResult result = inventoryService.stockOut(
                testProductId,
                45,
                TransactionType.SALE_OUT,
                "Large Sales Order #502",
                1
        );

        assertNotNull(result);
        assertEquals(45, result.getTotalDeducted());
        assertEquals(2, result.getAllocations().size(), "Should span across 2 batches");

        // First allocation: Batch B
        StockAllocation firstAlloc = result.getAllocations().get(0);
        assertTrue(firstAlloc.getBatchNumber().startsWith("BAT-B-"));
        assertEquals(20, firstAlloc.getAllocatedQuantity());
        assertEquals(0, firstAlloc.getRemainingBatchQuantity());

        // Second allocation: Batch A
        StockAllocation secondAlloc = result.getAllocations().get(1);
        assertTrue(secondAlloc.getBatchNumber().startsWith("BAT-A-"));
        assertEquals(25, secondAlloc.getAllocatedQuantity());
        assertEquals(75, secondAlloc.getRemainingBatchQuantity());

        // Verify Batch B is now DEPLETED
        Optional<Batch> batchB = inventoryService.getBatchById(firstAlloc.getBatchId());
        assertTrue(batchB.isPresent());
        assertEquals(0, batchB.get().getCurrentQuantity());
        assertEquals(BatchStatus.DEPLETED, batchB.get().getStatus());

        // Verify Batch A is ACTIVE with 75 units
        Optional<Batch> batchA = inventoryService.getBatchById(secondAlloc.getBatchId());
        assertTrue(batchA.isPresent());
        assertEquals(75, batchA.get().getCurrentQuantity());
        assertEquals(BatchStatus.ACTIVE, batchA.get().getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("5. Insufficient Stock: Reject stock out when requested quantity exceeds available")
    public void testInsufficientStockRejection() {
        // Currently available: 75 units in Batch A.
        // Requesting 500 units must fail without altering any inventory.
        int beforeStock = inventoryService.getTotalStockForProduct(testProductId);

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.stockOut(testProductId, 500, TransactionType.SALE_OUT, "Excessive Order", 1)
        );

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertEquals(500, ex.getRequested());

        // Verify no change in stock
        int afterStock = inventoryService.getTotalStockForProduct(testProductId);
        assertEquals(beforeStock, afterStock, "Stock must remain unchanged after rejected dispatch");
    }

    @Test
    @Order(6)
    @DisplayName("6. Validation Rules: Reject non-positive quantities and invalid prices")
    public void testValidationRules() {
        // Zero quantity stock out
        assertThrows(ValidationException.class, () ->
                inventoryService.stockOut(testProductId, 0, TransactionType.SALE_OUT, "Zero Qty", 1));

        // Negative quantity stock out
        assertThrows(ValidationException.class, () ->
                inventoryService.stockOut(testProductId, -5, TransactionType.SALE_OUT, "Negative Qty", 1));

        // Selling price less than purchase price in Stock In
        Batch invalidPriceBatch = new Batch(
                testProductId,
                testSupplierId,
                "BAT-PRICE-" + System.currentTimeMillis(),
                BigDecimal.valueOf(25.00), // Purchase price: $25
                BigDecimal.valueOf(15.00), // Selling price: $15 (invalid!)
                10,
                10,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(3)
        );

        ValidationException priceEx = assertThrows(ValidationException.class, () ->
                inventoryService.stockIn(invalidPriceBatch, 1, "Invalid pricing"));
        assertTrue(priceEx.getMessage().contains("Selling price"));

        // Duplicate batch number for same product
        long suffix = System.currentTimeMillis();
        String dupBatchNum = "BAT-DUP-" + (suffix % 1000000);
        Batch b1 = new Batch(testProductId, testSupplierId, dupBatchNum, BigDecimal.valueOf(10), BigDecimal.valueOf(15), 10, 10, null, LocalDate.now().plusDays(60));
        inventoryService.stockIn(b1, 1, "First");

        Batch b2 = new Batch(testProductId, testSupplierId, dupBatchNum, BigDecimal.valueOf(10), BigDecimal.valueOf(15), 10, 10, null, LocalDate.now().plusDays(60));
        assertThrows(DuplicateResourceException.class, () -> inventoryService.stockIn(b2, 1, "Second Duplicate"));
    }

    @Test
    @Order(7)
    @DisplayName("7. Stock Movement Audit Trail: Verify immutable ledger integrity")
    public void testTransactionLedgerIntegrity() {
        List<StockTransaction> txList = inventoryService.getAllTransactions(50);
        assertFalse(txList.isEmpty(), "Transactions ledger must contain recorded movements");

        // Verify required columns on transaction entries
        for (StockTransaction tx : txList) {
            assertNotNull(tx.getTransactionId(), "Tx ID must not be null");
            assertNotNull(tx.getProductId(), "Product ID must not be null");
            assertNotNull(tx.getBatchId(), "Batch ID must not be null");
            assertNotNull(tx.getTransactionType(), "Transaction Type must not be null");
            assertTrue(tx.getQuantity() > 0, "Transaction quantity must be positive");
            assertTrue(tx.getBalanceAfter() >= 0, "Balance after must be non-negative");
            assertNotNull(tx.getCreatedAt(), "Timestamp must be recorded");
        }
    }
}
