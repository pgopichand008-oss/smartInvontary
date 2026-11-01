package com.smartinventory.controller;

import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Product;
import com.smartinventory.service.ProductService;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller coordinating Product views and asynchronous service operations.
 */
public class ProductController {

    private final ProductService productService;

    public ProductController() {
        this(new ProductService());
    }

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public ProductService getProductService() {
        return productService;
    }

    /**
     * Asynchronously loads products filtered by category and active status.
     */
    public void loadProducts(Integer categoryId, Boolean activeOnly,
                             Consumer<List<Product>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Product>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Product> doInBackground() {
                return productService.searchProducts(null, categoryId, activeOnly);
            }

            @Override
            protected void done() {
                try {
                    List<Product> list = get();
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
     * Asynchronously searches products by keyword, category, and active status.
     */
    public void searchProducts(String keyword, Integer categoryId, Boolean activeOnly,
                               Consumer<List<Product>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Product>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Product> doInBackground() {
                return productService.searchProducts(keyword, categoryId, activeOnly);
            }

            @Override
            protected void done() {
                try {
                    List<Product> list = get();
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
     * Handles product creation with validation error callback.
     */
    public boolean handleCreateProduct(Product product, Consumer<String> onError) {
        try {
            productService.createProduct(product);
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
     * Handles product update with validation error callback.
     */
    public boolean handleUpdateProduct(Product product, Consumer<String> onError) {
        try {
            return productService.updateProduct(product);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles product deletion with error callback.
     */
    public boolean handleDeleteProduct(int productId, Consumer<String> onError) {
        try {
            return productService.deleteProduct(productId);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles toggling a product's active status with error callback.
     */
    public boolean handleToggleStatus(int productId, Consumer<String> onError) {
        try {
            return productService.toggleProductStatus(productId);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }
}
