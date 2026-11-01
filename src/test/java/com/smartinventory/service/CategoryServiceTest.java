package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Category;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for CategoryService business logic, validations, and database interactions.
 */
public class CategoryServiceTest {

    private static CategoryService categoryService;

    @BeforeAll
    public static void setUp() {
        categoryService = new CategoryService();
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    @Test
    @DisplayName("Create, find, update and delete a valid category")
    public void testCategoryLifecycle() {
        String uniqueName = "Test-Category-" + System.currentTimeMillis();
        String description = "Automated test category description";

        // 1. Create
        Category created = categoryService.createCategory(uniqueName, description);
        assertNotNull(created.getCategoryId(), "Generated category ID should not be null");
        assertEquals(uniqueName, created.getCategoryName());

        int catId = created.getCategoryId();

        // 2. Find by ID
        Optional<Category> found = categoryService.getCategoryById(catId);
        assertTrue(found.isPresent(), "Category should be found by ID");
        assertEquals(uniqueName, found.get().getCategoryName());

        // 3. Update
        String updatedName = uniqueName + "-Updated";
        boolean updated = categoryService.updateCategory(catId, updatedName, "Updated description");
        assertTrue(updated, "Update should return true");

        Optional<Category> foundUpdated = categoryService.getCategoryById(catId);
        assertTrue(foundUpdated.isPresent());
        assertEquals(updatedName, foundUpdated.get().getCategoryName());

        // 4. Search
        List<Category> searchResults = categoryService.searchCategories(updatedName);
        assertFalse(searchResults.isEmpty(), "Search should find the updated category");

        // 5. Delete
        boolean deleted = categoryService.deleteCategory(catId);
        assertTrue(deleted, "Delete should succeed");

        Optional<Category> foundDeleted = categoryService.getCategoryById(catId);
        assertFalse(foundDeleted.isPresent(), "Deleted category should no longer exist");
    }

    @Test
    @DisplayName("Duplicate category name throws DuplicateResourceException")
    public void testDuplicateCategoryPrevention() {
        String name = "Duplicate-Test-" + System.currentTimeMillis();
        Category cat = categoryService.createCategory(name, "First instance");
        assertNotNull(cat);

        try {
            assertThrows(DuplicateResourceException.class, () -> {
                categoryService.createCategory(name, "Second instance with same name");
            });
        } finally {
            categoryService.deleteCategory(cat.getCategoryId());
        }
    }

    @Test
    @DisplayName("Empty category name throws ValidationException")
    public void testEmptyCategoryNameValidation() {
        assertThrows(ValidationException.class, () -> {
            categoryService.createCategory("", "Valid description");
        });

        assertThrows(ValidationException.class, () -> {
            categoryService.createCategory("   ", "Valid description");
        });

        assertThrows(ValidationException.class, () -> {
            categoryService.createCategory(null, "Valid description");
        });
    }
}
