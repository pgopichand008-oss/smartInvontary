package com.smartinventory.controller;

import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Category;
import com.smartinventory.service.CategoryService;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller coordinating Category views and service operations.
 */
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController() {
        this(new CategoryService());
    }

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Asynchronously loads all categories to prevent UI freezing.
     */
    public void loadCategories(Consumer<List<Category>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Category>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Category> doInBackground() {
                return categoryService.getAllCategories();
            }

            @Override
            protected void done() {
                try {
                    List<Category> list = get();
                    onSuccess.accept(list);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously searches categories.
     */
    public void searchCategories(String keyword, Consumer<List<Category>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Category>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Category> doInBackground() {
                return categoryService.searchCategories(keyword);
            }

            @Override
            protected void done() {
                try {
                    List<Category> list = get();
                    onSuccess.accept(list);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Creates a new category.
     */
    public boolean handleCreateCategory(String name, String description, Consumer<String> onError) {
        try {
            categoryService.createCategory(name, description);
            return true;
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an existing category.
     */
    public boolean handleUpdateCategory(int id, String name, String description, Consumer<String> onError) {
        try {
            return categoryService.updateCategory(id, name, description);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a category by ID.
     */
    public boolean handleDeleteCategory(int id, Consumer<String> onError) {
        try {
            return categoryService.deleteCategory(id);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }
}
