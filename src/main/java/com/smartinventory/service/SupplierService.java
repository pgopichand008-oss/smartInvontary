package com.smartinventory.service;

import com.smartinventory.dao.SupplierDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceInUseException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service Layer enforcing business rules and validations for Suppliers.
 */
public class SupplierService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9\\-\\s()]{7,25}$");

    private final SupplierDAO supplierDAO;

    public SupplierService() {
        this(new SupplierDAO());
    }

    public SupplierService(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;
    }

    public Supplier createSupplier(String name, String contactPerson, String phone, String email, String address) {
        validateSupplierFields(name, contactPerson, phone, email);

        try {
            if (supplierDAO.existsByName(name, null)) {
                throw new DuplicateResourceException("Supplier with name '" + name.trim() + "' already exists.");
            }
            if (supplierDAO.existsByPhone(phone, null)) {
                throw new DuplicateResourceException("Supplier with phone number '" + phone.trim() + "' already exists.");
            }

            Supplier supplier = new Supplier(
                    name.trim(),
                    contactPerson != null ? contactPerson.trim() : null,
                    phone.trim(),
                    email != null ? email.trim() : null,
                    address != null ? address.trim() : null
            );

            Supplier created = supplierDAO.create(supplier);
            logger.info("Supplier created successfully with ID: {}", created.getSupplierId());
            return created;
        } catch (SQLException e) {
            logger.error("Database error while creating supplier: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to create supplier: " + e.getMessage(), e);
        }
    }

    public boolean updateSupplier(int id, String name, String contactPerson, String phone, String email, String address, boolean active) {
        if (id <= 0) {
            throw new ValidationException("Invalid supplier ID: " + id);
        }
        validateSupplierFields(name, contactPerson, phone, email);

        try {
            if (!supplierDAO.findById(id).isPresent()) {
                throw new ValidationException("Supplier not found with ID: " + id);
            }

            if (supplierDAO.existsByName(name, id)) {
                throw new DuplicateResourceException("Another supplier with name '" + name.trim() + "' already exists.");
            }
            if (supplierDAO.existsByPhone(phone, id)) {
                throw new DuplicateResourceException("Another supplier with phone number '" + phone.trim() + "' already exists.");
            }

            Supplier supplier = new Supplier(
                    id,
                    name.trim(),
                    contactPerson != null ? contactPerson.trim() : null,
                    phone.trim(),
                    email != null ? email.trim() : null,
                    address != null ? address.trim() : null,
                    active
            );

            boolean updated = supplierDAO.update(supplier);
            if (updated) {
                logger.info("Supplier updated successfully: ID {}", id);
            }
            return updated;
        } catch (SQLException e) {
            logger.error("Database error while updating supplier: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to update supplier: " + e.getMessage(), e);
        }
    }

    public boolean deleteSupplier(int id) {
        if (id <= 0) {
            throw new ValidationException("Invalid supplier ID: " + id);
        }

        try {
            int linkedBatches = supplierDAO.countBatchesFromSupplier(id);
            if (linkedBatches > 0) {
                throw new ResourceInUseException("Cannot permanently delete supplier: " + linkedBatches +
                        " batch(es) reference this supplier. Deactivate the supplier instead.");
            }

            boolean deleted = supplierDAO.delete(id);
            if (deleted) {
                logger.info("Supplier deleted successfully: ID {}", id);
            }
            return deleted;
        } catch (SQLException e) {
            logger.error("Database error while deleting supplier: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to delete supplier: " + e.getMessage(), e);
        }
    }

    public boolean setSupplierActiveStatus(int id, boolean active) {
        if (id <= 0) {
            throw new ValidationException("Invalid supplier ID: " + id);
        }
        try {
            boolean updated = supplierDAO.toggleStatus(id, active);
            if (updated) {
                logger.info("Supplier status updated: ID {} -> active={}", id, active);
            }
            return updated;
        } catch (SQLException e) {
            logger.error("Database error while updating supplier status: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to update supplier status: " + e.getMessage(), e);
        }
    }

    public Optional<Supplier> getSupplierById(int id) {
        try {
            return supplierDAO.findById(id);
        } catch (SQLException e) {
            logger.error("Database error while finding supplier by ID: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to find supplier: " + e.getMessage(), e);
        }
    }

    public List<Supplier> getAllSuppliers() {
        try {
            return supplierDAO.findAll();
        } catch (SQLException e) {
            logger.error("Database error while listing all suppliers: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to retrieve suppliers: " + e.getMessage(), e);
        }
    }

    public List<Supplier> getActiveSuppliers() {
        try {
            return supplierDAO.findActive();
        } catch (SQLException e) {
            logger.error("Database error while listing active suppliers: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to retrieve active suppliers: " + e.getMessage(), e);
        }
    }

    public List<Supplier> searchSuppliers(String keyword, Boolean activeOnly) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return (activeOnly != null && activeOnly) ? getActiveSuppliers() : getAllSuppliers();
        }
        try {
            return supplierDAO.search(keyword.trim(), activeOnly);
        } catch (SQLException e) {
            logger.error("Database error while searching suppliers: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to search suppliers: " + e.getMessage(), e);
        }
    }

    private void validateSupplierFields(String name, String contactPerson, String phone, String email) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Supplier company name is required.");
        }
        if (name.trim().length() < 2 || name.trim().length() > 150) {
            throw new ValidationException("Supplier name must be between 2 and 150 characters.");
        }
        if (contactPerson != null && contactPerson.trim().length() > 100) {
            throw new ValidationException("Contact person name cannot exceed 100 characters.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("Phone number is required.");
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Invalid phone number format. Provide a valid 7 to 25 digit number.");
        }
        if (email != null && !email.trim().isEmpty()) {
            if (email.trim().length() > 100) {
                throw new ValidationException("Email address cannot exceed 100 characters.");
            }
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                throw new ValidationException("Invalid email format (e.g., vendor@company.com).");
            }
        }
    }
}
