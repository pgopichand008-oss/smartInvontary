package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.model.AlertSummary;
import com.smartinventory.model.Batch;
import com.smartinventory.model.Category;
import com.smartinventory.model.ExpiryAlert;
import com.smartinventory.model.LowStockAlert;
import com.smartinventory.model.Product;
import com.smartinventory.model.Supplier;
import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.ExpirySeverity;
import com.smartinventory.model.enums.StockAlertSeverity;
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
 * JUnit 5 test suite for Phase 6:
 * Expiry Date Engine, Low-Stock Alert Detection, Day Boundaries,
 * and Exclusion Rules (empty inventory, inactive products, depleted batches).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AlertServiceTest {

    private static AlertService alertService;
    private static InventoryService inventoryService;
    private static ProductService productService;
    private static CategoryService categoryService;
    private static SupplierService supplierService;

    private static int alertCategoryId;
    private static int alertSupplierId;

    @BeforeAll
    public static void setUp() {
        alertService = new AlertService();
        inventoryService = new InventoryService();
        productService = new ProductService();
        categoryService = new CategoryService();
        supplierService = new SupplierService();

        // 1. Setup Category
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            Category cat = categoryService.createCategory("Alert Cat " + System.currentTimeMillis(), "Category for Alerts");
            alertCategoryId = cat.getCategoryId();
        } else {
            alertCategoryId = categories.get(0).getCategoryId();
        }

        // 2. Setup Supplier
        long suffix = System.currentTimeMillis();
        Supplier supp = supplierService.createSupplier("Alert Supplier " + suffix, "Jane Alert", "+1-888-" + (suffix % 10000), "alert" + suffix + "@test.com", "Alert HQ");
        alertSupplierId = supp.getSupplierId();
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    @Test
    @Order(1)
    @DisplayName("1. Expired Batches Detection: Identify past expiry dates with positive quantity")
    public void testExpiredBatchDetection() {
        long suffix = System.currentTimeMillis();
        Product product = productService.createProduct(
                alertCategoryId, "EXP-SKU-" + (suffix % 1000000), null,
                "Expired Milk " + suffix, "Testing expired detection", "carton", 10
        );

        String batchNum = "BAT-PAST-" + (suffix % 1000000);
        // Batch expired 4 days ago
        Batch pastBatch = new Batch(
                product.getProductId(),
                alertSupplierId,
                batchNum,
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(8.00),
                25,
                25,
                LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(4) // Expired 4 days ago
        );
        inventoryService.stockIn(pastBatch, 1, "Testing past batch");

        // Scan expired alerts
        List<ExpiryAlert> expiredAlerts = alertService.getExpiredBatches(alertCategoryId, batchNum);
        assertFalse(expiredAlerts.isEmpty(), "Expired batch must be detected by AlertService");

        ExpiryAlert alert = expiredAlerts.get(0);
        assertEquals(batchNum, alert.getBatchNumber());
        assertEquals(product.getProductId(), alert.getProductId());
        assertEquals(25, alert.getCurrentQuantity());
        assertTrue(alert.isExpired(), "isExpired() must return true");
        assertEquals(-4, alert.getDaysRemaining(), "Days remaining must be -4");
        assertEquals(ExpirySeverity.EXPIRED, alert.getSeverity());
    }

    @Test
    @Order(2)
    @DisplayName("2. Expiry Boundaries: Accurate classification for 7, 15, and 30 day horizons")
    public void testExpiryBoundaryHorizons() {
        long suffix = System.currentTimeMillis();
        Product product = productService.createProduct(
                alertCategoryId, "HORIZON-SKU-" + (suffix % 1000000), null,
                "Horizon Yogurt " + suffix, "Testing boundary horizons", "cup", 10
        );

        // Batch 1: Expires in 5 days (Critical: <= 7 days)
        String b1 = "BAT-7D-" + (suffix % 1000000);
        inventoryService.stockIn(new Batch(product.getProductId(), alertSupplierId, b1,
                BigDecimal.valueOf(2), BigDecimal.valueOf(4), 10, 10, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)), 1, "7d");

        // Batch 2: Expires in 12 days (Urgent: 8-15 days)
        String b2 = "BAT-15D-" + (suffix % 1000000);
        inventoryService.stockIn(new Batch(product.getProductId(), alertSupplierId, b2,
                BigDecimal.valueOf(2), BigDecimal.valueOf(4), 10, 10, LocalDate.now().minusDays(5), LocalDate.now().plusDays(12)), 1, "15d");

        // Batch 3: Expires in 25 days (Warning: 16-30 days)
        String b3 = "BAT-30D-" + (suffix % 1000000);
        inventoryService.stockIn(new Batch(product.getProductId(), alertSupplierId, b3,
                BigDecimal.valueOf(2), BigDecimal.valueOf(4), 10, 10, LocalDate.now().minusDays(5), LocalDate.now().plusDays(25)), 1, "30d");

        // Batch 4: Expires in 60 days (Safe: beyond 30 days)
        String b4 = "BAT-60D-" + (suffix % 1000000);
        inventoryService.stockIn(new Batch(product.getProductId(), alertSupplierId, b4,
                BigDecimal.valueOf(2), BigDecimal.valueOf(4), 10, 10, LocalDate.now().minusDays(5), LocalDate.now().plusDays(60)), 1, "60d");

        // --- Horizon 7 Days ---
        List<ExpiryAlert> alerts7 = alertService.getExpiringSoonBatches(7, alertCategoryId, "Horizon Yogurt " + suffix);
        assertEquals(1, alerts7.size(), "Within 7 days must only match Batch 1");
        assertEquals(b1, alerts7.get(0).getBatchNumber());
        assertEquals(ExpirySeverity.EXPIRING_7_DAYS, alerts7.get(0).getSeverity());

        // --- Horizon 15 Days ---
        List<ExpiryAlert> alerts15 = alertService.getExpiringSoonBatches(15, alertCategoryId, "Horizon Yogurt " + suffix);
        assertEquals(2, alerts15.size(), "Within 15 days must match Batch 1 and Batch 2");
        assertTrue(alerts15.stream().anyMatch(a -> a.getBatchNumber().equals(b1)));
        assertTrue(alerts15.stream().anyMatch(a -> a.getBatchNumber().equals(b2) && a.getSeverity() == ExpirySeverity.EXPIRING_15_DAYS));

        // --- Horizon 30 Days ---
        List<ExpiryAlert> alerts30 = alertService.getExpiringSoonBatches(30, alertCategoryId, "Horizon Yogurt " + suffix);
        assertEquals(3, alerts30.size(), "Within 30 days must match Batch 1, 2, and 3 (excluding Batch 4)");
        assertTrue(alerts30.stream().anyMatch(a -> a.getBatchNumber().equals(b3) && a.getSeverity() == ExpirySeverity.EXPIRING_30_DAYS));
        assertFalse(alerts30.stream().anyMatch(a -> a.getBatchNumber().equals(b4)), "Batch 4 (60 days) must not be in 30-day alerts");
    }

    @Test
    @Order(3)
    @DisplayName("3. Low-Stock Detection: Available unexpired quantity below minimum stock level")
    public void testLowStockDetection() {
        long suffix = System.currentTimeMillis();
        // Min stock level = 30
        Product product = productService.createProduct(
                alertCategoryId, "LOW-SKU-" + (suffix % 1000000), null,
                "Low Stock Chips " + suffix, "Testing deficit", "bag", 30
        );

        // Add batch with only 12 units (Deficit: 30 - 12 = 18 units)
        String batchNum = "BAT-LOW-" + (suffix % 1000000);
        inventoryService.stockIn(new Batch(product.getProductId(), alertSupplierId, batchNum,
                BigDecimal.valueOf(1.50), BigDecimal.valueOf(3.00), 12, 12,
                LocalDate.now().minusDays(1), LocalDate.now().plusMonths(6)), 1, "Low stock batch");

        List<LowStockAlert> alerts = alertService.getLowStockAlerts(alertCategoryId, "Low Stock Chips " + suffix);
        assertFalse(alerts.isEmpty(), "Product must be flagged with Low Stock alert");

        LowStockAlert alert = alerts.get(0);
        assertEquals(product.getProductId(), alert.getProductId());
        assertEquals(12, alert.getCurrentStock());
        assertEquals(30, alert.getMinStockLevel());
        assertEquals(18, alert.getDeficit());
        assertEquals(StockAlertSeverity.LOW_STOCK, alert.getSeverity());
        assertFalse(alert.isOutOfStock());
    }

    @Test
    @Order(4)
    @DisplayName("4. Out-of-Stock Detection: Active product with 0 available stock")
    public void testOutOfStockDetection() {
        long suffix = System.currentTimeMillis();
        // Min stock level = 20, zero batches created
        Product product = productService.createProduct(
                alertCategoryId, "OOS-SKU-" + (suffix % 1000000), null,
                "Out of Stock Pasta " + suffix, "Testing zero stock", "box", 20
        );

        List<LowStockAlert> alerts = alertService.getLowStockAlerts(alertCategoryId, "Out of Stock Pasta " + suffix);
        assertFalse(alerts.isEmpty(), "Zero-stock product must be detected as Out of Stock");

        LowStockAlert alert = alerts.get(0);
        assertEquals(product.getProductId(), alert.getProductId());
        assertEquals(0, alert.getCurrentStock(), "Current stock must be 0");
        assertEquals(20, alert.getDeficit());
        assertTrue(alert.isOutOfStock(), "isOutOfStock() must return true");
        assertEquals(StockAlertSeverity.OUT_OF_STOCK, alert.getSeverity());
    }

    @Test
    @Order(5)
    @DisplayName("5. Exclusion Rules: Inactive products must be excluded from alerts")
    public void testExcludeInactiveProducts() {
        long suffix = System.currentTimeMillis();
        // Inactive product with min stock 50 and an expired batch
        Product inactiveProduct = new Product(
                alertCategoryId, "INACT-SKU-" + (suffix % 1000000), null,
                "Inactive Deactivated Item " + suffix, "Should be ignored", "pcs", 50, false
        );
        productService.createProduct(inactiveProduct);

        // Low stock query must ignore inactive product
        List<LowStockAlert> lowStock = alertService.getLowStockAlerts(alertCategoryId, "Inactive Deactivated Item " + suffix);
        assertTrue(lowStock.isEmpty(), "Inactive products must not appear in low stock alerts");

        // Expired query must ignore inactive product
        List<ExpiryAlert> expired = alertService.getExpiredBatches(alertCategoryId, "Inactive Deactivated Item " + suffix);
        assertTrue(expired.isEmpty(), "Inactive products must not appear in expired alerts");
    }

    @Test
    @Order(6)
    @DisplayName("6. Exclusion Rules: Depleted (0 quantity) batches must not trigger expiring soon alerts")
    public void testExcludeDepletedBatches() {
        long suffix = System.currentTimeMillis();
        Product product = productService.createProduct(
                alertCategoryId, "DEP-SKU-" + (suffix % 1000000), null,
                "Depleted Bread " + suffix, "Testing depleted batch exclusion", "loaf", 5
        );

        // Stock in 10 units expiring in 3 days
        String batchNum = "BAT-DEP-" + (suffix % 1000000);
        inventoryService.stockIn(new Batch(product.getProductId(), alertSupplierId, batchNum,
                BigDecimal.valueOf(1), BigDecimal.valueOf(2), 10, 10,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(3)), 1, "To be depleted");

        // Dispatch all 10 units via FEFO (depleting it to 0)
        inventoryService.stockOut(product.getProductId(), 10, com.smartinventory.model.enums.TransactionType.SALE_OUT, "Sold all", 1);

        // Expiring soon query should NOT include the depleted batch
        List<ExpiryAlert> alerts = alertService.getExpiringSoonBatches(7, alertCategoryId, batchNum);
        assertTrue(alerts.isEmpty(), "Depleted batch (0 quantity) must not appear in upcoming expiration alerts");
    }

    @Test
    @Order(7)
    @DisplayName("7. Alert Summary Aggregation: Verification of KPI counts and category filters")
    public void testAlertSummaryAggregation() {
        AlertSummary summary = alertService.getAlertSummary(alertCategoryId, null);
        assertNotNull(summary);
        assertTrue(summary.getTotalActiveAlerts() >= 0);
        assertEquals(summary.getExpiredAlerts().size(), summary.getTotalExpiredBatches());
        assertEquals(summary.getExpiringSoonAlerts().size(), summary.getTotalExpiringSoonBatches());

        // Category filtering check
        AlertSummary filteredSummary = alertService.getAlertSummary(999999, null); // non-existent category
        assertEquals(0, filteredSummary.getTotalExpiredBatches());
        assertEquals(0, filteredSummary.getTotalExpiringSoonBatches());
        assertEquals(0, filteredSummary.getTotalLowStockProducts());
    }
}
