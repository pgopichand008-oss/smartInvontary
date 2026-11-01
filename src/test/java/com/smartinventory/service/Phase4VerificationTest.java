package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import com.smartinventory.model.Supplier;
import com.smartinventory.model.User;
import com.smartinventory.util.PasswordUtil;
import com.smartinventory.util.UserSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated test suite executing the 12-point Phase 4 verification checklist:
 * 1. Add a new product with valid details.
 * 2. Reject duplicate SKU.
 * 3. Reject duplicate barcode.
 * 4. Reject invalid or empty product names.
 * 5. Reject negative minimum stock levels.
 * 6. Verify category selection and category existence.
 * 7. Edit an existing product.
 * 8. Delete a product where deletion is allowed.
 * 9. Search products by name or SKU.
 * 10. Filter products by category.
 * 11. Verify that product records are correctly saved and retrieved from MySQL directly.
 * 12. Verify that existing Phase 1, 2 and 3 functionality still works.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase4VerificationTest {

    private static ProductService productService;
    private static CategoryService categoryService;
    private static SupplierService supplierService;
    private static AuthService authService;
    private static int verificationCategoryId;

    @BeforeAll
    public static void setUp() {
        productService = new ProductService();
        categoryService = new CategoryService();
        supplierService = new SupplierService();
        authService = new AuthService();

        // Ensure a dedicated valid Category exists for tests
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            Category created = categoryService.createCategory("Verification Category " + System.currentTimeMillis(), "Category for Phase 4 Verification");
            verificationCategoryId = created.getCategoryId();
        } else {
            verificationCategoryId = categories.get(0).getCategoryId();
        }
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    @Test
    @Order(1)
    @DisplayName("1. Add a new product with valid details")
    public void test01_AddProductWithValidDetails() {
        long suffix = System.currentTimeMillis();
        String sku = "V-SKU-" + (suffix % 1000000);
        String barcode = "V-BAR-" + (suffix % 1000000);
        String name = "Verification Product " + suffix;

        Product product = productService.createProduct(
                verificationCategoryId,
                sku,
                barcode,
                name,
                "Valid product description",
                "box",
                15
        );

        assertNotNull(product, "Created product must not be null");
        assertNotNull(product.getProductId(), "Product ID must be auto-generated");
        assertTrue(product.getProductId() > 0, "Product ID must be positive integer");
        assertEquals(sku, product.getSku(), "SKU must match input");
        assertEquals(barcode, product.getBarcode(), "Barcode must match input");
        assertEquals(name, product.getProductName(), "Product name must match input");
        assertEquals("box", product.getUnit(), "Unit must match input");
        assertEquals(15, product.getMinStockLevel(), "Min stock level must match input");
        assertTrue(product.isActive(), "Product must be active by default");

        // Clean up
        productService.deleteProduct(product.getProductId());
    }

    @Test
    @Order(2)
    @DisplayName("2. Reject duplicate SKU")
    public void test02_RejectDuplicateSku() {
        long suffix = System.currentTimeMillis();
        String duplicateSku = "DUP-SKU-" + (suffix % 1000000);

        Product first = productService.createProduct(
                verificationCategoryId, duplicateSku, null, "Original Product " + suffix, "Desc", "pcs", 10
        );
        assertNotNull(first.getProductId());

        try {
            // Attempt to insert another product with identical SKU
            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> productService.createProduct(
                            verificationCategoryId, duplicateSku, null, "Clash Product " + suffix, "Desc", "pcs", 10
                    ),
                    "Should reject duplicate SKU"
            );
            assertTrue(ex.getMessage().contains("already exists"), "Error message should mention already exists");
        } finally {
            productService.deleteProduct(first.getProductId());
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Reject duplicate barcode")
    public void test03_RejectDuplicateBarcode() {
        long suffix = System.currentTimeMillis();
        String sku1 = "SKU-B1-" + (suffix % 1000000);
        String sku2 = "SKU-B2-" + (suffix % 1000000);
        String duplicateBarcode = "DUP-BAR-" + (suffix % 1000000);

        Product first = productService.createProduct(
                verificationCategoryId, sku1, duplicateBarcode, "First Barcode Item " + suffix, "Desc", "pcs", 10
        );
        assertNotNull(first.getProductId());

        try {
            // Attempt to insert another product with identical Barcode
            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> productService.createProduct(
                            verificationCategoryId, sku2, duplicateBarcode, "Second Barcode Item " + suffix, "Desc", "pcs", 10
                    ),
                    "Should reject duplicate Barcode"
            );
            assertTrue(ex.getMessage().contains("already exists"), "Error message should mention already exists");
        } finally {
            productService.deleteProduct(first.getProductId());
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Reject invalid or empty product names")
    public void test04_RejectInvalidOrEmptyProductNames() {
        long suffix = System.currentTimeMillis();
        String sku = "NAME-SKU-" + (suffix % 1000000);

        // Empty name
        assertThrows(ValidationException.class, () -> productService.createProduct(
                verificationCategoryId, sku, null, "", "Desc", "pcs", 10
        ));

        // Whitespace-only name
        assertThrows(ValidationException.class, () -> productService.createProduct(
                verificationCategoryId, sku, null, "    ", "Desc", "pcs", 10
        ));

        // Null name
        assertThrows(ValidationException.class, () -> productService.createProduct(
                verificationCategoryId, sku, null, null, "Desc", "pcs", 10
        ));

        // Less than 2 characters
        assertThrows(ValidationException.class, () -> productService.createProduct(
                verificationCategoryId, sku, null, "A", "Desc", "pcs", 10
        ));
    }

    @Test
    @Order(5)
    @DisplayName("5. Reject negative minimum stock levels")
    public void test05_RejectNegativeMinimumStockLevels() {
        long suffix = System.currentTimeMillis();
        String sku = "STOCK-SKU-" + (suffix % 1000000);

        // Negative minimum stock
        ValidationException exNeg = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(
                        verificationCategoryId, sku, null, "Valid Name", "Desc", "pcs", -10
                )
        );
        assertTrue(exNeg.getMessage().contains("positive"), "Exception must mention positive minimum stock");

        // Zero minimum stock
        ValidationException exZero = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(
                        verificationCategoryId, sku, null, "Valid Name", "Desc", "pcs", 0
                )
        );
        assertTrue(exZero.getMessage().contains("positive"), "Exception must mention positive minimum stock");
    }

    @Test
    @Order(6)
    @DisplayName("6. Verify category selection and category existence")
    public void test06_VerifyCategorySelectionAndExistence() {
        long suffix = System.currentTimeMillis();
        String sku = "CAT-SKU-" + (suffix % 1000000);

        // Invalid negative/zero category ID
        assertThrows(ValidationException.class, () -> productService.createProduct(
                -1, sku, null, "Test Product", "Desc", "pcs", 10
        ));

        // Non-existent category ID (e.g. 999999)
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(
                        999999, sku, null, "Test Product", "Desc", "pcs", 10
                )
        );
        assertTrue(ex.getMessage().contains("category does not exist"), "Exception must mention category does not exist");

        // Valid category assigns correct categoryName in JOIN
        Product validProd = productService.createProduct(
                verificationCategoryId, sku, null, "Category Verified Prod " + suffix, "Desc", "pcs", 10
        );
        try {
            Optional<Product> fetched = productService.getProductById(validProd.getProductId());
            assertTrue(fetched.isPresent());
            assertNotNull(fetched.get().getCategoryName(), "Category name must be populated via SQL JOIN");
            assertFalse(fetched.get().getCategoryName().trim().isEmpty());
        } finally {
            productService.deleteProduct(validProd.getProductId());
        }
    }

    @Test
    @Order(7)
    @DisplayName("7. Edit an existing product")
    public void test07_EditExistingProduct() {
        long suffix = System.currentTimeMillis();
        String sku = "EDIT-SKU-" + (suffix % 1000000);

        Product p = productService.createProduct(
                verificationCategoryId, sku, null, "Original Name " + suffix, "Old Note", "pcs", 10
        );
        int id = p.getProductId();

        try {
            // Edit attributes
            p.setProductName("Updated Name " + suffix);
            p.setUnit("pack");
            p.setMinStockLevel(45);
            p.setBarcode("NEW-BAR-" + (suffix % 1000000));
            p.setDescription("New Note Updated");

            boolean updated = productService.updateProduct(p);
            assertTrue(updated, "Update must return true");

            // Verify persistence
            Optional<Product> fetched = productService.getProductById(id);
            assertTrue(fetched.isPresent());
            assertEquals("Updated Name " + suffix, fetched.get().getProductName());
            assertEquals("pack", fetched.get().getUnit());
            assertEquals(45, fetched.get().getMinStockLevel());
            assertEquals("NEW-BAR-" + (suffix % 1000000), fetched.get().getBarcode());
            assertEquals("New Note Updated", fetched.get().getDescription());
        } finally {
            productService.deleteProduct(id);
        }
    }

    @Test
    @Order(8)
    @DisplayName("8. Delete a product where deletion is allowed")
    public void test08_DeleteProductWhereDeletionAllowed() {
        long suffix = System.currentTimeMillis();
        String sku = "DEL-SKU-" + (suffix % 1000000);

        Product p = productService.createProduct(
                verificationCategoryId, sku, null, "Delete Me " + suffix, "Desc", "pcs", 10
        );
        int id = p.getProductId();

        // Product has 0 batches, deletion must succeed
        boolean deleted = productService.deleteProduct(id);
        assertTrue(deleted, "Delete must return true when no batches are linked");

        // Verify product no longer exists
        Optional<Product> check = productService.getProductById(id);
        assertFalse(check.isPresent(), "Deleted product must not be found");
    }

    @Test
    @Order(9)
    @DisplayName("9. Search products by name or SKU")
    public void test09_SearchProductsByNameOrSku() {
        long suffix = System.currentTimeMillis();
        String sku = "SRCH-SKU-" + (suffix % 1000000);
        String name = "Searchable Beverage " + suffix;

        Product p = productService.createProduct(
                verificationCategoryId, sku, null, name, "Desc", "bottle", 20
        );
        int id = p.getProductId();

        try {
            // Search by SKU
            List<Product> skuResults = productService.searchProducts(sku, null, null);
            assertFalse(skuResults.isEmpty(), "Search by exact SKU should find target");
            assertEquals(id, skuResults.get(0).getProductId());

            // Search by Name keyword
            List<Product> nameResults = productService.searchProducts("Searchable Beverage", null, null);
            assertFalse(nameResults.isEmpty(), "Search by Name substring should find target");
            assertTrue(nameResults.stream().anyMatch(prod -> prod.getProductId().equals(id)));
        } finally {
            productService.deleteProduct(id);
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. Filter products by category")
    public void test10_FilterProductsByCategory() {
        long suffix = System.currentTimeMillis();
        String sku = "FLTR-SKU-" + (suffix % 1000000);

        Product p = productService.createProduct(
                verificationCategoryId, sku, null, "Filter Category Item " + suffix, "Desc", "pcs", 10
        );
        int id = p.getProductId();

        try {
            List<Product> filtered = productService.getProductsByCategory(verificationCategoryId);
            assertFalse(filtered.isEmpty(), "Filtered products list must not be empty");
            assertTrue(filtered.stream().anyMatch(prod -> prod.getProductId().equals(id)),
                    "Target product must be present in category-filtered results");
            assertTrue(filtered.stream().allMatch(prod -> prod.getCategoryId().equals(verificationCategoryId)),
                    "All returned products must match the queried category ID");
        } finally {
            productService.deleteProduct(id);
        }
    }

    @Test
    @Order(11)
    @DisplayName("11. Verify product records correctly saved and retrieved from MySQL directly")
    public void test11_VerifyRecordsSavedAndRetrievedFromMySQLDirectly() throws SQLException {
        long suffix = System.currentTimeMillis();
        String sku = "SQL-SKU-" + (suffix % 1000000);
        String barcode = "SQL-BAR-" + (suffix % 1000000);
        String name = "Direct SQL Product " + suffix;

        Product created = productService.createProduct(
                verificationCategoryId, sku, barcode, name, "Direct SQL Verification", "can", 25
        );
        int prodId = created.getProductId();

        // Query MySQL directly via raw JDBC PreparedStatement to prove physical DB persistence
        String sql = "SELECT product_id, category_id, sku, barcode, product_name, unit, min_stock_level, is_active " +
                     "FROM products WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, prodId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Physical row must exist in MySQL database table 'products'");
                assertEquals(prodId, rs.getInt("product_id"), "MySQL product_id must match");
                assertEquals(verificationCategoryId, rs.getInt("category_id"), "MySQL category_id must match");
                assertEquals(sku, rs.getString("sku"), "MySQL sku must match");
                assertEquals(barcode, rs.getString("barcode"), "MySQL barcode must match");
                assertEquals(name, rs.getString("product_name"), "MySQL product_name must match");
                assertEquals("can", rs.getString("unit"), "MySQL unit must match");
                assertEquals(25, rs.getInt("min_stock_level"), "MySQL min_stock_level must match");
                assertTrue(rs.getBoolean("is_active"), "MySQL is_active must be true");
            }
        } finally {
            productService.deleteProduct(prodId);
        }
    }

    @Test
    @Order(12)
    @DisplayName("12. Verify that existing Phase 1, 2 and 3 functionality still works")
    public void test12_VerifyExistingPhase123FunctionalityStillWorks() throws SQLException {
        // --- Phase 1: Database Connectivity ---
        boolean dbHealthy = DatabaseConnection.testConnection();
        assertTrue(dbHealthy, "Phase 1: Database connection pool must be healthy and active");

        // --- Phase 2: Category CRUD ---
        long suffix = System.currentTimeMillis();
        String catName = "P2-Cat-" + suffix;
        Category createdCat = categoryService.createCategory(catName, "Phase 2 Regression Test");
        assertNotNull(createdCat.getCategoryId(), "Phase 2: Category created successfully");
        Optional<Category> foundCat = categoryService.getCategoryById(createdCat.getCategoryId());
        assertTrue(foundCat.isPresent(), "Phase 2: Category retrievable by ID");
        boolean catDeleted = categoryService.deleteCategory(createdCat.getCategoryId());
        assertTrue(catDeleted, "Phase 2: Category deletable");

        // --- Phase 2: Supplier CRUD ---
        String suppName = "P2-Supp-" + suffix;
        Supplier createdSupp = supplierService.createSupplier(suppName, "Contact", "+1-555-" + (suffix % 10000), "supp" + suffix + "@test.com", "Address");
        assertNotNull(createdSupp.getSupplierId(), "Phase 2: Supplier created successfully");
        Optional<Supplier> foundSupp = supplierService.getSupplierById(createdSupp.getSupplierId());
        assertTrue(foundSupp.isPresent(), "Phase 2: Supplier retrievable by ID");
        boolean suppDeleted = supplierService.deleteSupplier(createdSupp.getSupplierId());
        assertTrue(suppDeleted, "Phase 2: Supplier deletable");

        // --- Phase 3: Cryptography & Authentication Engine ---
        String rawPassword = "password123";
        String hashed = PasswordUtil.hashPassword(rawPassword);
        assertTrue(PasswordUtil.checkPassword(rawPassword, hashed), "Phase 3: BCrypt hashing and verification must pass");

        User loggedInUser = authService.login("admin", "admin123");
        assertNotNull(loggedInUser, "Phase 3: Admin login must succeed");
        assertTrue(UserSession.getInstance().isAdmin(), "Phase 3: UserSession must recognize Admin role");
        authService.logout();
        assertNull(UserSession.getInstance().getCurrentUser(), "Phase 3: Logout must clear session");
    }
}
