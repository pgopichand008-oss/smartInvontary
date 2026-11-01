package com.smartinventory.controller;

import com.smartinventory.model.dashboard.DashboardMetrics;
import com.smartinventory.service.DashboardService;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Controller executing asynchronous database operations for the Executive Dashboard.
 */
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController() {
        this(new DashboardService());
    }

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    /**
     * Asynchronously loads comprehensive KPI metrics, category distributions, and monthly movements.
     */
    public void loadDashboardMetrics(Consumer<DashboardMetrics> onSuccess, Consumer<String> onError) {
        SwingWorker<DashboardMetrics, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardMetrics doInBackground() {
                return dashboardService.getDashboardMetrics();
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
