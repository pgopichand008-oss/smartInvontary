package com.smartinventory.service;

import com.smartinventory.dao.AlertDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.model.AlertSummary;
import com.smartinventory.model.ExpiryAlert;
import com.smartinventory.model.LowStockAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service Layer for real-time surveillance of expired lots, upcoming expirations,
 * and inventory low-stock deficits.
 */
public class AlertService {

    public interface AlertChangeListener {
        void onAlertsChanged();
    }

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private final AlertDAO alertDAO;
    private final List<AlertChangeListener> listeners = new CopyOnWriteArrayList<>();

    public AlertService() {
        this(new AlertDAO());
    }

    public AlertService(AlertDAO alertDAO) {
        this.alertDAO = alertDAO;
    }

    public void addAlertChangeListener(AlertChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeAlertChangeListener(AlertChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyAlertsChanged() {
        for (AlertChangeListener listener : listeners) {
            try {
                listener.onAlertsChanged();
            } catch (Exception e) {
                logger.warn("Error notifying alert listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Synchronizes batch database status flags (EXPIRED / EXPIRING_SOON) with current date.
     */
    public void synchronizeStatuses() {
        try {
            alertDAO.synchronizeBatchStatuses();
        } catch (SQLException e) {
            logger.error("Failed to synchronize batch statuses: {}", e.getMessage(), e);
        }
    }

    /**
     * Compiles complete aggregated alert summary for KPI badges and surveillance centers.
     */
    public AlertSummary getAlertSummary(Integer categoryId, String keyword) {
        try {
            synchronizeStatuses();
            List<ExpiryAlert> expired = alertDAO.findExpiredBatches(categoryId, keyword);
            List<ExpiryAlert> expiringSoon = alertDAO.findExpiringSoonBatches(30, categoryId, keyword);
            List<LowStockAlert> lowStock = alertDAO.findLowStockAlerts(categoryId, keyword);

            return new AlertSummary(expired, expiringSoon, lowStock);
        } catch (SQLException e) {
            logger.error("Failed to compile alert summary: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to retrieve inventory alerts: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all expired active batches for active products.
     */
    public List<ExpiryAlert> getExpiredBatches(Integer categoryId, String keyword) {
        try {
            return alertDAO.findExpiredBatches(categoryId, keyword);
        } catch (SQLException e) {
            logger.error("Failed to query expired batches: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to fetch expired batches: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves upcoming expiring batches within specified horizon (e.g. 7, 15, or 30 days).
     */
    public List<ExpiryAlert> getExpiringSoonBatches(int maxDays, Integer categoryId, String keyword) {
        try {
            return alertDAO.findExpiringSoonBatches(maxDays, categoryId, keyword);
        } catch (SQLException e) {
            logger.error("Failed to query expiring batches: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to fetch expiring batches: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all low-stock and out-of-stock active products based on eligible unexpired quantities.
     */
    public List<LowStockAlert> getLowStockAlerts(Integer categoryId, String keyword) {
        try {
            return alertDAO.findLowStockAlerts(categoryId, keyword);
        } catch (SQLException e) {
            logger.error("Failed to query low stock alerts: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to fetch low stock alerts: " + e.getMessage(), e);
        }
    }
}
