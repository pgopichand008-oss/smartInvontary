package com.smartinventory.controller;

import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.Supplier;
import com.smartinventory.service.SupplierService;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller coordinating Supplier views and service operations.
 */
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController() {
        this(new SupplierService());
    }

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * Asynchronously loads suppliers based on status filter.
     */
    public void loadSuppliers(Boolean activeOnly, Consumer<List<Supplier>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Supplier>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Supplier> doInBackground() {
                if (activeOnly != null && activeOnly) {
                    return supplierService.getActiveSuppliers();
                }
                return supplierService.getAllSuppliers();
            }

            @Override
            protected void done() {
                try {
                    List<Supplier> list = get();
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
     * Asynchronously searches suppliers.
     */
    public void searchSuppliers(String keyword, Boolean activeOnly, Consumer<List<Supplier>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<Supplier>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Supplier> doInBackground() {
                return supplierService.searchSuppliers(keyword, activeOnly);
            }

            @Override
            protected void done() {
                try {
                    List<Supplier> list = get();
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
     * Creates a new supplier.
     */
    public boolean handleCreateSupplier(String name, String contact, String phone, String email, String address, Consumer<String> onError) {
        try {
            supplierService.createSupplier(name, contact, phone, email, address);
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
     * Updates an existing supplier.
     */
    public boolean handleUpdateSupplier(int id, String name, String contact, String phone, String email, String address, boolean active, Consumer<String> onError) {
        try {
            return supplierService.updateSupplier(id, name, contact, phone, email, address, active);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a supplier by ID.
     */
    public boolean handleDeleteSupplier(int id, Consumer<String> onError) {
        try {
            return supplierService.deleteSupplier(id);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggles the active status of a supplier.
     */
    public boolean handleToggleActiveStatus(int id, boolean active, Consumer<String> onError) {
        try {
            return supplierService.setSupplierActiveStatus(id, active);
        } catch (ValidationException e) {
            onError.accept(e.getMessage());
            return false;
        } catch (Exception e) {
            onError.accept("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }
}
