package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.dao.UserDAO;
import com.smartinventory.exception.AuthenticationException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.InsufficientStockException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.*;
import com.smartinventory.model.dashboard.DashboardMetrics;
import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.model.enums.UserRole;
import com.smartinventory.model.report.ReportData;
import com.smartinventory.model.report.ReportType;
import com.smartinventory.util.PasswordUtil;
import com.smartinventory.util.UserSession;
import com.smartinventory.util.export.CsvReportExporter;
import com.smartinventory.util.export.ExcelReportExporter;
import com.smartinventory.util.export.PdfReportExporter;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete End-to-End Manual Testing Verification Test Suite.
 * Covers all 12 E2E workflow phases required for the audit.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndManualVerificationTest {

    private static AuthService authService;
    private static UserDAO userDAO;
    private static CategoryService categoryService;
    private static SupplierService supplierService;
    private static ProductService productService;
    private static InventoryService inventoryService;
    private static AlertService alertService;
    private static DashboardService dashboardService;
    private static ReportService reportService;

    // State persisted across sequential ordered tests
    private static int e2eCategoryId;
    private static int e2eSupplierId;
    private static int e2eProductId;
    private static int batch1Id;
    private static int batch2Id;
    private static int batch3Id;
    private static User testManagerUser;
    private static User testCashierUser;

    @BeforeAll
    public static void setUp() throws SQLException {
        authService = new AuthService();
        userDAO = new UserDAO();
        categoryService = new CategoryService();
        supplierService = new SupplierService();
        productService = new ProductService();
        inventoryService = new InventoryService();
        alertService = new AlertService();
        dashboardService = new DashboardService();
        reportService = new ReportService();

        // Ensure admin user has valid password hash
        Optional<User> adminOpt = userDAO.findByUsername("admin");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            if (!PasswordUtil.checkPassword("admin123", admin.getPasswordHash())) {
                userDAO.updatePassword(admin.getUserId(), PasswordUtil.hashPassword("admin123"));
            }
        }
    }

    @AfterAll
    public static void tearDown() {
        authService.logout();
        DatabaseConnection.closePool();
    }

    // =========================================================================
    // 1. Authentication & Session Management
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("E2E Step 3: Verify Admin Login, Logout, and Invalid Credentials")
    public void test01_VerifyLoginAndLogout() {
        // 1. Successful Admin Login
        User admin = authService.login("admin", "admin123");
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(UserSession.getInstance().isLoggedIn());
        assertTrue(UserSession.getInstance().isAdmin());

        // 2. Logout
        authService.logout();
        assertFalse(UserSession.getInstance().isLoggedIn());
        assertNull(UserSession.getInstance().getCurrentUser());

        // 3. Bad credentials
        assertThrows(AuthenticationException.class, () -> authService.login("admin", "wrongpassword"));
        assertThrows(AuthenticationException.class, () -> authService.login("non_existent_user", "password123"));
        assertFalse(UserSession.getInstance().isLoggedIn());

        // Log back in as admin for subsequent management steps
        authService.login("admin", "admin123");
    }

    // =========================================================================
    // 2. Category Creation, Editing, Deletion & Constraints
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("E2E Step 4A: Category Lifecycle (Create, Edit, Duplicate Constraint, Query)")
    public void test02_CategoryLifecycle() {
        long timestamp = System.currentTimeMillis();
        String catName = "E2E Dairy & Cold " + (timestamp % 100000);

        // 1. Create Category
        Category category = categoryService.createCategory(catName, "Dairy and refrigerated items for E2E testing");
        assertNotNull(category);
        assertTrue(category.getCategoryId() > 0);
        e2eCategoryId = category.getCategoryId();

        // 2. Reject Duplicate Name
        assertThrows(DuplicateResourceException.class, () -> {
            categoryService.createCategory(catName, "Duplicate attempt");
        });

        // 3. Edit Category
        boolean updated = categoryService.updateCategory(e2eCategoryId, catName + " Updated", "Updated refrigerated description");
        assertTrue(updated);

        // 4. Verification in DB
        Optional<Category> fetched = categoryService.getCategoryById(e2eCategoryId);
        assertTrue(fetched.isPresent());
        assertEquals(catName + " Updated", fetched.get().getCategoryName());
    }

    // =========================================================================
    // 3. Supplier Creation, Editing, Status Toggling
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("E2E Step 4B: Supplier Lifecycle (Create, Edit, Toggle Active, Validation)")
    public void test03_SupplierLifecycle() {
        long timestamp = System.currentTimeMillis();
        String supplierName = "E2E Global Distributors " + (timestamp % 100000);

        // 1. Create Supplier
        String initialPhone = "+1-800-" + (timestamp % 90000 + 10000);
        Supplier supplier = supplierService.createSupplier(
                supplierName,
                "John E2E Contact",
                initialPhone,
                "orders" + timestamp + "@e2eglobal.com",
                "100 Logistics Blvd, Warehouse District"
        );
        assertNotNull(supplier);
        assertTrue(supplier.getSupplierId() > 0);
        e2eSupplierId = supplier.getSupplierId();

        // 2. Reject empty name or invalid phone
        assertThrows(ValidationException.class, () -> {
            supplierService.createSupplier("", "Contact", "123", "email@test.com", "Address");
        });

        // 3. Edit Supplier
        String updatedPhone = "+1-888-" + (timestamp % 90000 + 10000);
        boolean updated = supplierService.updateSupplier(
                e2eSupplierId,
                supplierName + " Corp",
                "Jane Senior Contact",
                updatedPhone,
                "senior.orders" + timestamp + "@e2eglobal.com",
                "102 Logistics Blvd, Suite 400",
                true
        );
        assertTrue(updated);
        Supplier fetched = supplierService.getSupplierById(e2eSupplierId).orElseThrow();
        assertEquals("Jane Senior Contact", fetched.getContactPerson());

        // 4. Toggle Status (Active -> Inactive -> Active)
        supplierService.setSupplierActiveStatus(e2eSupplierId, false);
        Supplier inactive = supplierService.getSupplierById(e2eSupplierId).orElseThrow();
        assertFalse(inactive.isActive());

        supplierService.setSupplierActiveStatus(e2eSupplierId, true);
        Supplier activeAgain = supplierService.getSupplierById(e2eSupplierId).orElseThrow();
        assertTrue(activeAgain.isActive());
    }

    // =========================================================================
    // 4. Product Creation and Management
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("E2E Step 5: Product Lifecycle (Create, Unique SKU & Barcode, Min Stock Validation)")
    public void test04_ProductLifecycle() {
        long timestamp = System.currentTimeMillis();
        String sku = "SKU-E2E-" + (timestamp % 1000000);
        String barcode = "BAR-E2E-" + (timestamp % 1000000);
        String productName = "E2E Organic Greek Yogurt " + timestamp;

        // 1. Create Product
        Product product = productService.createProduct(
                e2eCategoryId,
                sku,
                barcode,
                productName,
                "500g cup with live active cultures",
                "cup",
                20 // minStockLevel
        );
        assertNotNull(product);
        assertTrue(product.getProductId() > 0);
        e2eProductId = product.getProductId();

        // 2. Reject duplicate SKU
        assertThrows(DuplicateResourceException.class, () -> {
            productService.createProduct(e2eCategoryId, sku, "OTHER-BARCODE", "Different Name", "Desc", "pcs", 10);
        });

        // 3. Reject duplicate Barcode
        assertThrows(DuplicateResourceException.class, () -> {
            productService.createProduct(e2eCategoryId, "OTHER-SKU", barcode, "Different Name", "Desc", "pcs", 10);
        });

        // 4. Reject negative min stock level
        assertThrows(ValidationException.class, () -> {
            productService.createProduct(e2eCategoryId, "SKU-NEG", "BAR-NEG", "Product", "Desc", "pcs", -5);
        });

        // 5. Update Product
        Product toUpdate = new Product(
                e2eProductId,
                e2eCategoryId,
                sku,
                barcode,
                productName + " (Updated)",
                "500g cup updated description",
                "cup",
                25, // updated min stock level
                true
        );
        boolean updated = productService.updateProduct(toUpdate);
        assertTrue(updated);

        Product fetched = productService.getProductById(e2eProductId).orElseThrow();
        assertEquals(25, fetched.getMinStockLevel());
        assertEquals(productName + " (Updated)", fetched.getProductName());
    }

    // =========================================================================
    // 5. Stock In with Multiple Batches & Different Expiry Dates
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("E2E Step 6: Multi-Batch Stock In with staggered expiry dates")
    public void test05_StockInMultipleBatches() {
        long timestamp = System.currentTimeMillis();

        // Batch 1: Furthest Expiry (6 months out)
        // Qty: 30, Cost: 2.00, Sell: 3.50, Expiry: +180 days
        LocalDate expiryBatch1 = LocalDate.now().plusMonths(6);
        Batch b1 = new Batch(
                e2eProductId,
                e2eSupplierId,
                "BATCH-E2E-180D-" + (timestamp % 10000),
                new BigDecimal("2.00"),
                new BigDecimal("3.50"),
                30,
                30,
                LocalDate.now().minusDays(5),
                expiryBatch1
        );
        Batch createdB1 = inventoryService.stockIn(b1, 1, "PO-E2E-001 Stock-in Shipment A");
        assertNotNull(createdB1);
        assertEquals(30, createdB1.getCurrentQuantity());
        assertEquals(BatchStatus.ACTIVE, createdB1.getStatus());
        batch1Id = createdB1.getBatchId();

        // Batch 2: Medium Expiry (60 days out)
        // Qty: 20, Cost: 2.10, Sell: 3.75, Expiry: +60 days
        LocalDate expiryBatch2 = LocalDate.now().plusDays(60);
        Batch b2 = new Batch(
                e2eProductId,
                e2eSupplierId,
                "BATCH-E2E-60D-" + (timestamp % 10000),
                new BigDecimal("2.10"),
                new BigDecimal("3.75"),
                20,
                20,
                LocalDate.now().minusDays(10),
                expiryBatch2
        );
        Batch createdB2 = inventoryService.stockIn(b2, 1, "PO-E2E-002 Stock-in Shipment B");
        assertNotNull(createdB2);
        assertEquals(20, createdB2.getCurrentQuantity());
        batch2Id = createdB2.getBatchId();

        // Batch 3: Earliest Expiry (15 days out)
        // Qty: 15, Cost: 1.95, Sell: 3.40, Expiry: +15 days
        LocalDate expiryBatch3 = LocalDate.now().plusDays(15);
        Batch b3 = new Batch(
                e2eProductId,
                e2eSupplierId,
                "BATCH-E2E-15D-" + (timestamp % 10000),
                new BigDecimal("1.95"),
                new BigDecimal("3.40"),
                15,
                15,
                LocalDate.now().minusDays(15),
                expiryBatch3
        );
        Batch createdB3 = inventoryService.stockIn(b3, 1, "PO-E2E-003 Stock-in Shipment C");
        assertNotNull(createdB3);
        assertEquals(15, createdB3.getCurrentQuantity());
        batch3Id = createdB3.getBatchId();

        // Total Stock across all 3 batches = 30 + 20 + 15 = 65
        int totalStock = inventoryService.getTotalStockForProduct(e2eProductId);
        assertEquals(65, totalStock, "Total product stock must equal sum of all received batches");

        // Verify Stock Transactions logged in DB
        List<StockTransaction> txns = inventoryService.getAllTransactions(50);
        long inTxCount = txns.stream()
                .filter(t -> t.getProductId().equals(e2eProductId) && t.getTransactionType() == TransactionType.PURCHASE_IN)
                .count();
        assertTrue(inTxCount >= 3, "At least 3 PURCHASE_IN transactions should be recorded for this product");
    }

    // =========================================================================
    // 6. Stock Out and FEFO (First Expiry, First Out) Allocation
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("E2E Step 7: Stock Out with FEFO multi-batch allocation")
    public void test06_StockOutWithFefoAllocation() {
        // We request a stock out of 25 units.
        // Batches by expiry:
        // 1. batch3 (15 days left): has 15 units -> allocates ALL 15 -> remaining becomes 0 (DEPLETED)
        // 2. batch2 (60 days left): has 20 units -> allocates 10 -> remaining becomes 10 (ACTIVE)
        // 3. batch1 (180 days left): has 30 units -> untouched (30 remaining)

        StockOutResult result = inventoryService.stockOut(
                e2eProductId,
                25,
                TransactionType.SALE_OUT,
                "Invoice-E2E-001 Customer Retail Checkout",
                1
        );

        assertNotNull(result);
        assertEquals(25, result.getTotalDeducted());
        assertEquals(2, result.getAllocations().size(), "Must allocate across exactly 2 batches");

        // Verify first allocation is from batch3 (earliest expiry)
        StockAllocation alloc1 = result.getAllocations().get(0);
        assertEquals(batch3Id, alloc1.getBatchId());
        assertEquals(15, alloc1.getAllocatedQuantity());

        // Verify second allocation is from batch2
        StockAllocation alloc2 = result.getAllocations().get(1);
        assertEquals(batch2Id, alloc2.getBatchId());
        assertEquals(10, alloc2.getAllocatedQuantity());

        // Verify batch states in DB
        Batch updatedB3 = inventoryService.getBatchById(batch3Id).orElseThrow();
        assertEquals(0, updatedB3.getCurrentQuantity());
        assertEquals(BatchStatus.DEPLETED, updatedB3.getStatus(), "Exhausted batch must be DEPLETED");

        Batch updatedB2 = inventoryService.getBatchById(batch2Id).orElseThrow();
        assertEquals(10, updatedB2.getCurrentQuantity());
        assertEquals(BatchStatus.ACTIVE, updatedB2.getStatus());

        Batch updatedB1 = inventoryService.getBatchById(batch1Id).orElseThrow();
        assertEquals(30, updatedB1.getCurrentQuantity());

        // Total product stock remaining = 65 - 25 = 40
        assertEquals(40, inventoryService.getTotalStockForProduct(e2eProductId));
    }

    // =========================================================================
    // 7. Insufficient Stock Handling
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("E2E Step 8: Insufficient Stock Handling and Transaction Rollback")
    public void test07_InsufficientStockHandling() {
        // Current active stock is 40 (30 + 10). Attempting to stock out 50 must fail and rollback.
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.stockOut(
                    e2eProductId,
                    50,
                    TransactionType.SALE_OUT,
                    "Failed Out Attempt Exceeding Stock",
                    1
            );
        });

        // Verify state is completely preserved (rollback succeeded)
        assertEquals(40, inventoryService.getTotalStockForProduct(e2eProductId));
        Batch b1 = inventoryService.getBatchById(batch1Id).orElseThrow();
        assertEquals(30, b1.getCurrentQuantity());
        Batch b2 = inventoryService.getBatchById(batch2Id).orElseThrow();
        assertEquals(10, b2.getCurrentQuantity());
    }

    // =========================================================================
    // 8. Expiry Alerts and Low-Stock Surveillance
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("E2E Step 9: Expiry Alerts and Low-Stock Deficit Detection")
    public void test08_AlertsSurveillance() {
        // 1. Expiring Soon Surveillance: batch2 (expiry in 60 days)
        List<ExpiryAlert> expiringSoon = alertService.getExpiringSoonBatches(65, null, null);
        assertNotNull(expiringSoon);
        boolean containsB2 = expiringSoon.stream().anyMatch(a -> a.getBatchId() == batch2Id);
        assertTrue(containsB2, "Expiring soon list within 65 days must include Batch 2");

        // 2. Insert an intentionally expired batch to verify expired alert detection
        Batch expiredLot = new Batch(
                e2eProductId,
                e2eSupplierId,
                "EXP-TEST-" + (System.currentTimeMillis() % 100000),
                new BigDecimal("1.50"),
                new BigDecimal("2.50"),
                5,
                5,
                LocalDate.now().minusMonths(3),
                LocalDate.now().minusDays(2) // Expired 2 days ago
        );
        Batch createdExpired = inventoryService.stockIn(expiredLot, 1, "Expired Batch for Alert Verification");

        List<ExpiryAlert> expiredAlerts = alertService.getExpiredBatches(null, null);
        assertNotNull(expiredAlerts);
        boolean foundExpired = expiredAlerts.stream().anyMatch(a -> a.getBatchId() == createdExpired.getBatchId());
        assertTrue(foundExpired, "Expired alert engine must identify the overdue batch");

        // 3. Low-Stock Alert Verification
        // Deduct 25 more units from active stock: active stock becomes 40 - 25 = 15.
        // Product min_stock_level is 25. 15 <= 25, so product is now in LOW STOCK.
        inventoryService.stockOut(e2eProductId, 25, TransactionType.SALE_OUT, "Deduct to trigger Low Stock", 1);

        List<LowStockAlert> lowStockAlerts = alertService.getLowStockAlerts(null, null);
        assertNotNull(lowStockAlerts);
        Optional<LowStockAlert> productLowStock = lowStockAlerts.stream()
                .filter(a -> a.getProductId() == e2eProductId)
                .findFirst();

        assertTrue(productLowStock.isPresent(), "Product must be flagged in Low Stock Alerts");
        LowStockAlert alert = productLowStock.get();
        assertEquals(25, alert.getMinStockLevel());
        assertTrue(alert.getCurrentStock() <= 25);
        assertTrue(alert.getDeficit() >= 0);
    }

    // =========================================================================
    // 9. Dashboard Metrics and Database Alignment
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("E2E Step 10: Dashboard Metrics Reconciliation Against Live Database")
    public void test09_DashboardMetricsReconciliation() throws SQLException {
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();
        assertNotNull(metrics);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check 1: Total Active Products
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE is_active = TRUE");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(rs.getInt(1), metrics.getTotalProducts(), "Dashboard active product count must match DB");
            }

            // Check 2: Total Available Stock In Hand (active, non-expired)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COALESCE(SUM(b.current_quantity), 0) " +
                    "FROM product_batches b " +
                    "JOIN products p ON b.product_id = p.product_id " +
                    "WHERE p.is_active = TRUE " +
                    "  AND b.current_quantity > 0 " +
                    "  AND b.status IN ('ACTIVE', 'EXPIRING_SOON') " +
                    "  AND b.expiry_date >= CURRENT_DATE");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(rs.getInt(1), metrics.getTotalAvailableStock(), "Dashboard stock quantity must match DB");
            }

            // Check 3: Active Expired Batches
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM product_batches b " +
                    "JOIN products p ON b.product_id = p.product_id " +
                    "WHERE p.is_active = TRUE AND b.current_quantity > 0 AND b.status != 'DEPLETED' AND b.expiry_date < CURRENT_DATE");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(rs.getInt(1), metrics.getTotalExpiredBatches(), "Dashboard expired batches count must match DB");
            }
        }
    }

    // =========================================================================
    // 10. Export Reports (CSV, Excel, PDF)
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("E2E Step 11: Export CSV, Excel, and PDF Reports and Verify Output Files")
    public void test10_ExportReportsValidation() throws IOException {
        // 1. Inventory Valuation Report
        ReportData valuationReport = reportService.generateInventoryValuationReport(e2eCategoryId, "E2EAuditor");
        assertNotNull(valuationReport);
        assertEquals(ReportType.INVENTORY_VALUATION, valuationReport.getReportType());

        // Test PDF Export
        File tempPdf = File.createTempFile("e2e_valuation_", ".pdf");
        tempPdf.deleteOnExit();
        PdfReportExporter.exportToFile(tempPdf, valuationReport);
        assertTrue(tempPdf.exists() && tempPdf.length() > 0, "PDF export file must exist and be non-empty");
        byte[] pdfBytes = Files.readAllBytes(tempPdf.toPath());
        assertTrue(new String(pdfBytes, 0, 10).startsWith("%PDF-1.4"), "Must have valid PDF header");

        // Test Excel SpreadsheetML Export
        File tempXls = File.createTempFile("e2e_valuation_", ".xls");
        tempXls.deleteOnExit();
        ExcelReportExporter.exportToFile(tempXls, valuationReport);
        assertTrue(tempXls.exists() && tempXls.length() > 0, "Excel export file must exist and be non-empty");
        String xmlContent = Files.readString(tempXls.toPath());
        assertTrue(xmlContent.contains("xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\""), "Must contain SpreadsheetML XML namespace");

        // Test CSV with UTF-8 BOM Export
        File tempCsv = File.createTempFile("e2e_valuation_", ".csv");
        tempCsv.deleteOnExit();
        CsvReportExporter.exportToFile(tempCsv, valuationReport);
        assertTrue(tempCsv.exists() && tempCsv.length() > 0, "CSV export file must exist and be non-empty");
        byte[] csvBytes = Files.readAllBytes(tempCsv.toPath());
        assertEquals((byte) 0xEF, csvBytes[0], "CSV must have UTF-8 BOM byte 1");
        assertEquals((byte) 0xBB, csvBytes[1], "CSV must have UTF-8 BOM byte 2");
        assertEquals((byte) 0xBF, csvBytes[2], "CSV must have UTF-8 BOM byte 3");

        // 2. Expiry Surveillance Report Export
        ReportData expiryReport = reportService.generateExpirySurveillanceReport(null, 60, "E2EAuditor");
        File tempExpiryPdf = File.createTempFile("e2e_expiry_", ".pdf");
        tempExpiryPdf.deleteOnExit();
        PdfReportExporter.exportToFile(tempExpiryPdf, expiryReport);
        assertTrue(tempExpiryPdf.exists() && tempExpiryPdf.length() > 0);

        // 3. Low Stock Report Export
        ReportData lowStockReport = reportService.generateLowStockReport(null, "E2EAuditor");
        File tempLowStockCsv = File.createTempFile("e2e_lowstock_", ".csv");
        tempLowStockCsv.deleteOnExit();
        CsvReportExporter.exportToFile(tempLowStockCsv, lowStockReport);
        assertTrue(tempLowStockCsv.exists() && tempLowStockCsv.length() > 0);
    }

    // =========================================================================
    // 11. Role-Based Access and Input Validations
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("E2E Step 12: Role-Based Access and Invalid Input Handling")
    public void test11_RoleBasedAccessAndValidation() throws SQLException {
        long timestamp = System.currentTimeMillis();

        // 1. Create a Manager user
        String mgrUsername = "mgr_" + (timestamp % 10000);
        User manager = new User(0, mgrUsername, PasswordUtil.hashPassword("manager123"), "Test Manager", "mgr@store.local", UserRole.MANAGER, true, null, null);
        User createdMgr = userDAO.create(manager);
        testManagerUser = createdMgr;

        // 2. Create a Cashier user
        String cshUsername = "cashier_" + (timestamp % 10000);
        User cashier = new User(0, cshUsername, PasswordUtil.hashPassword("cashier123"), "Test Cashier", "cashier@store.local", UserRole.CASHIER, true, null, null);
        User createdCsh = userDAO.create(cashier);
        testCashierUser = createdCsh;

        // 3. Verify Manager login and permissions
        authService.login(mgrUsername, "manager123");
        UserSession mgrSession = UserSession.getInstance();
        assertTrue(mgrSession.isLoggedIn());
        assertTrue(mgrSession.isManagerOrAdmin());
        assertFalse(mgrSession.isAdmin());

        // 4. Verify Cashier login and permissions
        authService.login(cshUsername, "cashier123");
        UserSession cshSession = UserSession.getInstance();
        assertTrue(cshSession.isLoggedIn());
        assertFalse(cshSession.isAdmin());
        assertFalse(cshSession.isManagerOrAdmin());
        assertEquals(UserRole.CASHIER, cshSession.getCurrentUser().getRole());

        // 5. Test invalid inputs (negative quantity, past manufacturing, etc.)
        assertThrows(ValidationException.class, () -> {
            Batch invalidBatch = new Batch(
                    e2eProductId,
                    e2eSupplierId,
                    "INV-BATCH",
                    new BigDecimal("10.00"),
                    new BigDecimal("5.00"), // selling price less than purchase price
                    10,
                    10,
                    LocalDate.now(),
                    LocalDate.now().plusMonths(1)
            );
            inventoryService.stockIn(invalidBatch, 1, "Invalid price test");
        });

        assertThrows(ValidationException.class, () -> {
            Batch invalidBatch = new Batch(
                    e2eProductId,
                    e2eSupplierId,
                    "INV-BATCH-2",
                    new BigDecimal("10.00"),
                    new BigDecimal("15.00"),
                    -10, // negative quantity
                    -10,
                    LocalDate.now(),
                    LocalDate.now().plusMonths(1)
            );
            inventoryService.stockIn(invalidBatch, 1, "Negative quantity test");
        });
    }

    // =========================================================================
    // 12. Deletion Safety and Clean Reference Cascading
    // =========================================================================
    @Test
    @Order(12)
    @DisplayName("E2E Step 4C: Deletion Safety - Prevent deleting category or product with active dependencies")
    public void test12_DeletionSafetyChecks() throws Exception {
        // Log in as Admin for cleanup tests
        authService.login("admin", "admin123");

        // Attempting to delete category that has products attached must fail safely
        assertThrows(Exception.class, () -> {
            categoryService.deleteCategory(e2eCategoryId);
        });

        // Attempting to delete product that has stock batches attached must fail safely
        assertThrows(Exception.class, () -> {
            productService.deleteProduct(e2eProductId);
        });

        // Clean up unreferenced temporary users
        if (testManagerUser != null) {
            userDAO.delete(testManagerUser.getUserId());
        }
        if (testCashierUser != null) {
            userDAO.delete(testCashierUser.getUserId());
        }
    }
}
