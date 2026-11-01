package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for ProductService business logic, validations, and database interactions.
 */
public class ProductServiceTest {

    private static ProductService productService;
    private static CategoryService categoryService;
    private static int testCategoryId;

    @BeforeAll
    public static void setUp() {
        productService = new ProductService();
        categoryService = new CategoryService();

        // Ensure a valid category exists for product testing
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            Category created = categoryService.createCategory("Test Cat for Products " + System.currentTimeMillis(), "Setup Category");
            testCategoryId = created.getCategoryId();
        } else {
            testCategoryId = categories.get(0).getCategoryId();
        }
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    @Test
    @DisplayName("Complete Product CRUD Lifecycle: Create, Read, Update, Toggle, Delete")
    public void testProductLifecycle() {
        long suffix = System.currentTimeMillis();
        String sku = "SKU-TEST-" + (suffix % 1000000);
        String barcode = "BAR-" + (suffix % 1000000);
        String name = "Test Organic Juice " + suffix;

        // 1. Create
        Product created = productService.createProduct(
                testCategoryId,
                sku,
                barcode,
                name,
                "Freshly squeezed juice",
                "bottle",
                20
        );

        assertNotNull(created.getProductId(), "Generated product ID should not be null");
        assertEquals(sku, created.getSku());
        assertEquals(barcode, created.getBarcode());
        assertEquals(name, created.getProductName());
        assertEquals("bottle", created.getUnit());
        assertEquals(20, created.getMinStockLevel());
        assertTrue(created.isActive(), "Product should be active by default");

        int prodId = created.getProductId();

        // 2. Read by ID
        Optional<Product> foundById = productService.getProductById(prodId);
        assertTrue(foundById.isPresent(), "Product should be found by ID");
        assertEquals(sku, foundById.get().getSku());

        // 3. Read by SKU
        Optional<Product> foundBySku = productService.getProductBySku(sku);
        assertTrue(foundBySku.isPresent(), "Product should be found by SKU");
        assertEquals(prodId, foundBySku.get().getProductId());

        // 4. Read by Barcode
        Optional<Product> foundByBarcode = productService.getProductByBarcode(barcode);
        assertTrue(foundByBarcode.isPresent(), "Product should be found by Barcode");
        assertEquals(prodId, foundByBarcode.get().getProductId());

        // 5. Update
        created.setProductName(name + " Updated");
        created.setUnit("can");
        created.setMinStockLevel(35);
        boolean updated = productService.updateProduct(created);
        assertTrue(updated, "Product update should succeed");

        Optional<Product> updatedFound = productService.getProductById(prodId);
        assertTrue(updatedFound.isPresent());
        assertEquals(name + " Updated", updatedFound.get().getProductName());
        assertEquals("can", updatedFound.get().getUnit());
        assertEquals(35, updatedFound.get().getMinStockLevel());

        // 6. Toggle Status
        boolean toggled = productService.toggleProductStatus(prodId);
        assertTrue(toggled, "Toggle status should succeed");
        Optional<Product> inactiveProd = productService.getProductById(prodId);
        assertTrue(inactiveProd.isPresent());
        assertFalse(inactiveProd.get().isActive(), "Product should now be inactive");

        // 7. Delete
        boolean deleted = productService.deleteProduct(prodId);
        assertTrue(deleted, "Product deletion should succeed");

        Optional<Product> deletedCheck = productService.getProductById(prodId);
        assertFalse(deletedCheck.isPresent(), "Deleted product should not be found");
    }

    @Test
    @DisplayName("Validation Rules: Rejection of invalid inputs")
    public void testValidationRules() {
        long suffix = System.currentTimeMillis();

        // Null product
        assertThrows(ValidationException.class, () -> productService.createProduct(null));

        // Non-existent category
        assertThrows(ValidationException.class, () -> productService.createProduct(
                999999, "SKU-VAL-" + suffix, null, "Name", "Desc", "pcs", 10
        ));

        // Invalid SKU: blank
        assertThrows(ValidationException.class, () -> productService.createProduct(
                testCategoryId, "   ", null, "Valid Name", "Desc", "pcs", 10
        ));

        // Invalid SKU: too short
        assertThrows(ValidationException.class, () -> productService.createProduct(
                testCategoryId, "X", null, "Valid Name", "Desc", "pcs", 10
        ));

        // Invalid Product Name: blank
        assertThrows(ValidationException.class, () -> productService.createProduct(
                testCategoryId, "SKU-VAL-" + suffix, null, "   ", "Desc", "pcs", 10
        ));

        // Invalid Min Stock Level: zero
        assertThrows(ValidationException.class, () -> productService.createProduct(
                testCategoryId, "SKU-VAL-" + suffix, null, "Valid Name", "Desc", "pcs", 0
        ));

        // Invalid Min Stock Level: negative
        assertThrows(ValidationException.class, () -> productService.createProduct(
                testCategoryId, "SKU-VAL-" + suffix, null, "Valid Name", "Desc", "pcs", -5
        ));
    }

    @Test
    @DisplayName("Duplicate Prevention: Prevent duplicate SKU")
    public void testDuplicateSkuRejection() {
        long suffix = System.currentTimeMillis();
        String duplicateSku = "SKU-DUP-" + (suffix % 1000000);

        Product first = productService.createProduct(
                testCategoryId, duplicateSku, null, "First Product " + suffix, "Desc", "pcs", 10
        );
        assertNotNull(first.getProductId());

        try {
            assertThrows(DuplicateResourceException.class, () -> productService.createProduct(
                    testCategoryId, duplicateSku, null, "Second Product " + suffix, "Desc", "pcs", 15
            ));
        } finally {
            productService.deleteProduct(first.getProductId());
        }
    }

    @Test
    @DisplayName("Duplicate Prevention: Prevent duplicate Barcode")
    public void testDuplicateBarcodeRejection() {
        long suffix = System.currentTimeMillis();
        String sku1 = "SKU-B1-" + (suffix % 1000000);
        String sku2 = "SKU-B2-" + (suffix % 1000000);
        String duplicateBarcode = "BAR-DUP-" + (suffix % 1000000);

        Product first = productService.createProduct(
                testCategoryId, sku1, duplicateBarcode, "First Barcode Prod " + suffix, "Desc", "pcs", 10
        );
        assertNotNull(first.getProductId());

        try {
            assertThrows(DuplicateResourceException.class, () -> productService.createProduct(
                    testCategoryId, sku2, duplicateBarcode, "Second Barcode Prod " + suffix, "Desc", "pcs", 15
            ));
        } finally {
            productService.deleteProduct(first.getProductId());
        }
    }

    @Test
    @DisplayName("Search and Category Filtering")
    public void testSearchAndFilter() {
        long suffix = System.currentTimeMillis();
        String sku = "SKU-SRCH-" + (suffix % 1000000);
        String uniqueKeyword = "SpecialKeyword" + suffix;

        Product p = productService.createProduct(
                testCategoryId, sku, null, "Juice " + uniqueKeyword, "Test Description", "pcs", 10
        );
        assertNotNull(p.getProductId());

        try {
            // Search by keyword
            List<Product> keywordResults = productService.searchProducts(uniqueKeyword, null, null);
            assertFalse(keywordResults.isEmpty(), "Should find product by unique keyword");
            assertEquals(sku, keywordResults.get(0).getSku());

            // Search by category
            List<Product> categoryResults = productService.searchProducts(uniqueKeyword, testCategoryId, null);
            assertFalse(categoryResults.isEmpty(), "Should find product filtered by category");

            // Filter active only
            List<Product> activeResults = productService.searchProducts(uniqueKeyword, testCategoryId, true);
            assertFalse(activeResults.isEmpty(), "Should find active product");
        } finally {
            productService.deleteProduct(p.getProductId());
        }
    }
}
