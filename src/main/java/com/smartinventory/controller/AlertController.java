package com.smartinventory.controller;

import com.smartinventory.model.AlertSummary;
import com.smartinventory.model.ExpiryAlert;
import com.smartinventory.model.LowStockAlert;
import com.smartinventory.service.AlertService;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller coordinating live inventory alert surveillance using asynchronous SwingWorkers.
 */
public class AlertController {

    private final AlertService alertService;

    public AlertController() {
        this(new AlertService());
    }

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    public AlertService getAlertService() {
        return alertService;
    }

    /**
     * Asynchronously loads comprehensive alert summary for KPI dashboard cards and tables.
     */
    public void loadAlertSummary(Integer categoryId, String keyword,
                                 Consumer<AlertSummary> onSuccess, Consumer<String> onError) {
        SwingWorker<AlertSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected AlertSummary doInBackground() {
                return alertService.getAlertSummary(categoryId, keyword);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously queries upcoming expiring batches for a specific day horizon.
     */
    public void loadExpiringBatches(int maxDays, Integer categoryId, String keyword,
                                   Consumer<List<ExpiryAlert>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<ExpiryAlert>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ExpiryAlert> doInBackground() {
                return alertService.getExpiringSoonBatches(maxDays, categoryId, keyword);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously queries expired active lots.
     */
    public void loadExpiredBatches(Integer categoryId, String keyword,
                                  Consumer<List<ExpiryAlert>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<ExpiryAlert>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ExpiryAlert> doInBackground() {
                return alertService.getExpiredBatches(categoryId, keyword);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Asynchronously queries low-stock and depleted active products.
     */
    public void loadLowStockAlerts(Integer categoryId, String keyword,
                                  Consumer<List<LowStockAlert>> onSuccess, Consumer<String> onError) {
        SwingWorker<List<LowStockAlert>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LowStockAlert> doInBackground() {
                return alertService.getLowStockAlerts(categoryId, keyword);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
