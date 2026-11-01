package com.smartinventory.service;

import com.smartinventory.dao.CategoryDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceInUseException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service Layer enforcing business rules and validations for Categories.
 */
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this(new CategoryDAO());
    }

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public Category createCategory(String name, String description) {
        validateCategoryFields(name, description);

        try {
            if (categoryDAO.existsByName(name, null)) {
                throw new DuplicateResourceException("Category with name '" + name.trim() + "' already exists.");
            }

            Category category = new Category(name.trim(), description != null ? description.trim() : null);
            Category created = categoryDAO.create(category);
            logger.info("Category created successfully with ID: {}", created.getCategoryId());
            return created;
        } catch (SQLException e) {
            logger.error("Database error while creating category: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to create category: " + e.getMessage(), e);
        }
    }

    public boolean updateCategory(int id, String name, String description) {
        if (id <= 0) {
            throw new ValidationException("Invalid category ID: " + id);
        }
        validateCategoryFields(name, description);

        try {
            if (!categoryDAO.findById(id).isPresent()) {
                throw new ValidationException("Category not found with ID: " + id);
            }

            if (categoryDAO.existsByName(name, id)) {
                throw new DuplicateResourceException("Another category with name '" + name.trim() + "' already exists.");
            }

            Category category = new Category(id, name.trim(), description != null ? description.trim() : null);
            boolean updated = categoryDAO.update(category);
            if (updated) {
                logger.info("Category updated successfully: ID {}", id);
            }
            return updated;
        } catch (SQLException e) {
            logger.error("Database error while updating category: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to update category: " + e.getMessage(), e);
        }
    }

    public boolean deleteCategory(int id) {
        if (id <= 0) {
            throw new ValidationException("Invalid category ID: " + id);
        }

        try {
            int linkedProducts = categoryDAO.countProductsInCategory(id);
            if (linkedProducts > 0) {
                throw new ResourceInUseException("Cannot delete category: " + linkedProducts +
                        " product(s) are assigned to this category. Please reassign or delete the products first.");
            }

            boolean deleted = categoryDAO.delete(id);
            if (deleted) {
                logger.info("Category deleted successfully: ID {}", id);
            }
            return deleted;
        } catch (SQLException e) {
            logger.error("Database error while deleting category: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to delete category: " + e.getMessage(), e);
        }
    }

    public Optional<Category> getCategoryById(int id) {
        try {
            return categoryDAO.findById(id);
        } catch (SQLException e) {
            logger.error("Database error while finding category by ID: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to find category: " + e.getMessage(), e);
        }
    }

    public List<Category> getAllCategories() {
        try {
            return categoryDAO.findAll();
        } catch (SQLException e) {
            logger.error("Database error while listing all categories: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to retrieve categories: " + e.getMessage(), e);
        }
    }

    public List<Category> searchCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCategories();
        }
        try {
            return categoryDAO.search(keyword.trim());
        } catch (SQLException e) {
            logger.error("Database error while searching categories: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to search categories: " + e.getMessage(), e);
        }
    }

    private void validateCategoryFields(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name is required and cannot be blank.");
        }
        if (name.trim().length() < 2 || name.trim().length() > 100) {
            throw new ValidationException("Category name must be between 2 and 100 characters.");
        }
        if (description != null && description.trim().length() > 255) {
            throw new ValidationException("Description cannot exceed 255 characters.");
        }
    }
}
