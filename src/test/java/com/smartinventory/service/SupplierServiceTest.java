package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for SupplierService business logic, validations, and database interactions.
 */
public class SupplierServiceTest {

    private static SupplierService supplierService;

    @BeforeAll
    public static void setUp() {
        supplierService = new SupplierService();
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    @Test
    @DisplayName("Create, find, update and delete a valid supplier")
    public void testSupplierLifecycle() {
        long suffix = System.currentTimeMillis();
        String name = "Supplier-" + suffix;
        String phone = "+1-555-" + (suffix % 10000);
        String email = "contact" + suffix + "@supplier.com";

        // 1. Create
        Supplier created = supplierService.createSupplier(name, "Alice Smith", phone, email, "123 Industrial Park");
        assertNotNull(created.getSupplierId(), "Generated supplier ID should not be null");
        assertEquals(name, created.getSupplierName());
        assertTrue(created.isActive());

        int suppId = created.getSupplierId();

        // 2. Find by ID
        Optional<Supplier> found = supplierService.getSupplierById(suppId);
        assertTrue(found.isPresent());
        assertEquals(phone, found.get().getPhone());

        // 3. Update
        String updatedName = name + "-Updated";
        boolean updated = supplierService.updateSupplier(suppId, updatedName, "Bob Jones", phone, email, "456 Market St", true);
        assertTrue(updated);

        Optional<Supplier> foundUpdated = supplierService.getSupplierById(suppId);
        assertTrue(foundUpdated.isPresent());
        assertEquals(updatedName, foundUpdated.get().getSupplierName());
        assertEquals("Bob Jones", foundUpdated.get().getContactPerson());

        // 4. Toggle Status
        boolean deactivated = supplierService.setSupplierActiveStatus(suppId, false);
        assertTrue(deactivated);
        assertFalse(supplierService.getSupplierById(suppId).get().isActive());

        // 5. Search
        List<Supplier> searchResults = supplierService.searchSuppliers(updatedName, null);
        assertFalse(searchResults.isEmpty());

        // 6. Delete
        boolean deleted = supplierService.deleteSupplier(suppId);
        assertTrue(deleted);
        assertFalse(supplierService.getSupplierById(suppId).isPresent());
    }

    @Test
    @DisplayName("Duplicate supplier name or phone throws DuplicateResourceException")
    public void testDuplicateSupplierPrevention() {
        long suffix = System.currentTimeMillis();
        String name = "DupSupplier-" + suffix;
        String phone = "+1-888-" + (suffix % 10000);

        Supplier s = supplierService.createSupplier(name, "Rep One", phone, "dup@test.com", "Address 1");
        assertNotNull(s);

        try {
            // Duplicate Name
            assertThrows(DuplicateResourceException.class, () -> {
                supplierService.createSupplier(name, "Rep Two", "+1-999-0000", "dup2@test.com", "Address 2");
            });

            // Duplicate Phone
            assertThrows(DuplicateResourceException.class, () -> {
                supplierService.createSupplier("Another Name " + suffix, "Rep Three", phone, "dup3@test.com", "Address 3");
            });
        } finally {
            supplierService.deleteSupplier(s.getSupplierId());
        }
    }

    @Test
    @DisplayName("Invalid phone or email formats throw ValidationException")
    public void testFormatValidations() {
        // Empty company name
        assertThrows(ValidationException.class, () -> {
            supplierService.createSupplier("", "John", "+1-555-0100", "test@test.com", "Addr");
        });

        // Invalid phone (too short or letters)
        assertThrows(ValidationException.class, () -> {
            supplierService.createSupplier("Valid Company", "John", "123", "test@test.com", "Addr");
        });

        // Invalid email format
        assertThrows(ValidationException.class, () -> {
            supplierService.createSupplier("Valid Company 2", "John", "+1-555-0100", "not-an-email", "Addr");
        });
    }
}
