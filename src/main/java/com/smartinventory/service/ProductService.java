package com.smartinventory.service;

import com.smartinventory.dao.CategoryDAO;
import com.smartinventory.dao.ProductDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceInUseException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service Layer enforcing business rules and validations for Products.
 */
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;

    public ProductService() {
        this(new ProductDAO(), new CategoryDAO());
    }

    public ProductService(ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
    }

    /**
     * Creates a new product after performing all business validations and duplicate checks.
     */
    public Product createProduct(Product product) {
        if (product == null) {
            throw new ValidationException("Product cannot be null.");
        }

        validateProductFields(product);

        try {
            // Check Category existence
            if (!categoryDAO.findById(product.getCategoryId()).isPresent()) {
                throw new ValidationException("Selected category does not exist (ID: " + product.getCategoryId() + ").");
            }

            // Check SKU uniqueness
            if (productDAO.existsBySku(product.getSku(), null)) {
                throw new DuplicateResourceException("Product with SKU '" + product.getSku().trim() + "' already exists.");
            }

            // Check Barcode uniqueness (if provided)
            if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
                if (productDAO.existsByBarcode(product.getBarcode().trim(), null)) {
                    throw new DuplicateResourceException("Product with Barcode '" + product.getBarcode().trim() + "' already exists.");
                }
            }

            Product created = productDAO.create(product);
            logger.info("Product created successfully: ID {}, SKU: '{}'", created.getProductId(), created.getSku());
            return created;
        } catch (SQLException e) {
            logger.error("Database error while creating product: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to create product: " + e.getMessage(), e);
        }
    }

    /**
     * Overloaded convenience method for creating products.
     */
    public Product createProduct(int categoryId, String sku, String barcode, String name,
                                 String description, String unit, int minStockLevel) {
        Product product = new Product(categoryId, sku, barcode, name, description, unit, minStockLevel, true);
        return createProduct(product);
    }

    /**
     * Updates an existing product with validation and duplicate exclusion.
     */
    public boolean updateProduct(Product product) {
        if (product == null || product.getProductId() == null || product.getProductId() <= 0) {
            throw new ValidationException("Invalid product ID for update.");
        }

        validateProductFields(product);

        try {
            // Verify product exists
            if (!productDAO.findById(product.getProductId()).isPresent()) {
                throw new ValidationException("Product not found with ID: " + product.getProductId());
            }

            // Verify category exists
            if (!categoryDAO.findById(product.getCategoryId()).isPresent()) {
                throw new ValidationException("Selected category does not exist (ID: " + product.getCategoryId() + ").");
            }

            // Check SKU duplicate excluding current product
            if (productDAO.existsBySku(product.getSku(), product.getProductId())) {
                throw new DuplicateResourceException("Another product with SKU '" + product.getSku().trim() + "' already exists.");
            }

            // Check Barcode duplicate excluding current product
            if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
                if (productDAO.existsByBarcode(product.getBarcode().trim(), product.getProductId())) {
                    throw new DuplicateResourceException("Another product with Barcode '" + product.getBarcode().trim() + "' already exists.");
                }
            }

            boolean updated = productDAO.update(product);
            if (updated) {
                logger.info("Product updated successfully: ID {}, SKU: '{}'", product.getProductId(), product.getSku());
            }
            return updated;
        } catch (SQLException e) {
            logger.error("Database error while updating product: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to update product: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a product if no associated inventory batches exist.
     */
    public boolean deleteProduct(int productId) {
        if (productId <= 0) {
            throw new ValidationException("Invalid product ID: " + productId);
        }

        try {
            // Verify existence
            if (!productDAO.findById(productId).isPresent()) {
                throw new ValidationException("Product not found with ID: " + productId);
            }

            // Check relational integrity with batches
            int linkedBatches = productDAO.countBatchesForProduct(productId);
            if (linkedBatches > 0) {
                throw new ResourceInUseException("Cannot delete product: " + linkedBatches +
                        " inventory batch(es) are associated with it. Please deactivate the product instead.");
            }

            boolean deleted = productDAO.delete(productId);
            if (deleted) {
                logger.info("Product deleted successfully: ID {}", productId);
            }
            return deleted;
        } catch (SQLException e) {
            logger.error("Database error while deleting product: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to delete product: " + e.getMessage(), e);
        }
    }

    /**
     * Toggles the active status of a product.
     */
    public boolean toggleProductStatus(int productId) {
        if (productId <= 0) {
            throw new ValidationException("Invalid product ID: " + productId);
        }

        try {
            Optional<Product> opt = productDAO.findById(productId);
            if (!opt.isPresent()) {
                throw new ValidationException("Product not found with ID: " + productId);
            }

            Product product = opt.get();
            product.setActive(!product.isActive());
            return productDAO.update(product);
        } catch (SQLException e) {
            logger.error("Database error while toggling product status: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to toggle product status: " + e.getMessage(), e);
        }
    }

    public Optional<Product> getProductById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        try {
            return productDAO.findById(id);
        } catch (SQLException e) {
            logger.error("Database error fetching product by ID {}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch product: " + e.getMessage(), e);
        }
    }

    public Optional<Product> getProductBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return productDAO.findBySku(sku);
        } catch (SQLException e) {
            logger.error("Database error fetching product by SKU '{}': {}", sku, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch product by SKU: " + e.getMessage(), e);
        }
    }

    public Optional<Product> getProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return productDAO.findByBarcode(barcode);
        } catch (SQLException e) {
            logger.error("Database error fetching product by Barcode '{}': {}", barcode, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch product by Barcode: " + e.getMessage(), e);
        }
    }

    public List<Product> getAllProducts() {
        try {
            return productDAO.findAll();
        } catch (SQLException e) {
            logger.error("Database error fetching all products: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to fetch products: " + e.getMessage(), e);
        }
    }

    public List<Product> getActiveProducts() {
        return searchProducts(null, null, true);
    }

    public List<Product> getProductsByCategory(int categoryId) {
        if (categoryId <= 0) {
            throw new ValidationException("Invalid category ID: " + categoryId);
        }
        try {
            return productDAO.findByCategoryId(categoryId);
        } catch (SQLException e) {
            logger.error("Database error fetching products by category {}: {}", categoryId, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch products for category: " + e.getMessage(), e);
        }
    }

    public List<Product> searchProducts(String keyword, Integer categoryId, Boolean activeOnly) {
        try {
            return productDAO.search(keyword, categoryId, activeOnly);
        } catch (SQLException e) {
            logger.error("Database error searching products: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to search products: " + e.getMessage(), e);
        }
    }

    /**
     * Validates domain constraints on product fields.
     */
    private void validateProductFields(Product product) {
        if (product.getCategoryId() == null || product.getCategoryId() <= 0) {
            throw new ValidationException("A valid product category must be selected.");
        }

        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            throw new ValidationException("Product SKU is required.");
        }
        if (product.getSku().trim().length() < 2 || product.getSku().trim().length() > 50) {
            throw new ValidationException("Product SKU must be between 2 and 50 characters.");
        }

        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            throw new ValidationException("Product name is required.");
        }
        if (product.getProductName().trim().length() < 2 || product.getProductName().trim().length() > 150) {
            throw new ValidationException("Product name must be between 2 and 150 characters.");
        }

        if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
            if (product.getBarcode().trim().length() > 100) {
                throw new ValidationException("Barcode cannot exceed 100 characters.");
            }
        }

        if (product.getUnit() == null || product.getUnit().trim().isEmpty()) {
            product.setUnit("pcs");
        } else if (product.getUnit().trim().length() > 20) {
            throw new ValidationException("Unit cannot exceed 20 characters.");
        }

        if (product.getMinStockLevel() <= 0) {
            throw new ValidationException("Minimum stock level must be a positive integer greater than zero.");
        }
    }
}
