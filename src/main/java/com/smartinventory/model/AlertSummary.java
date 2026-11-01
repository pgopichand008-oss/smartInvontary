package com.smartinventory.model;

import com.smartinventory.model.enums.ExpirySeverity;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated summary of live expiry warnings and inventory deficits.
 */
public class AlertSummary {

    private final List<ExpiryAlert> expiredAlerts;
    private final List<ExpiryAlert> expiringSoonAlerts;
    private final List<LowStockAlert> lowStockAlerts;

    private int totalExpiredBatches;
    private int totalExpiringSoonBatches;
    private int count7Days;
    private int count15Days;
    private int count30Days;
    private int totalLowStockProducts;
    private int totalOutOfStockProducts;

    public AlertSummary(List<ExpiryAlert> expiredAlerts,
                        List<ExpiryAlert> expiringSoonAlerts,
                        List<LowStockAlert> lowStockAlerts) {
        this.expiredAlerts = expiredAlerts != null ? expiredAlerts : Collections.emptyList();
        this.expiringSoonAlerts = expiringSoonAlerts != null ? expiringSoonAlerts : Collections.emptyList();
        this.lowStockAlerts = lowStockAlerts != null ? lowStockAlerts : Collections.emptyList();

        computeCounts();
    }

    private void computeCounts() {
        this.totalExpiredBatches = expiredAlerts.size();
        this.totalExpiringSoonBatches = expiringSoonAlerts.size();

        this.count7Days = 0;
        this.count15Days = 0;
        this.count30Days = 0;

        for (ExpiryAlert alert : expiringSoonAlerts) {
            if (alert.getSeverity() == ExpirySeverity.EXPIRING_7_DAYS) {
                count7Days++;
            } else if (alert.getSeverity() == ExpirySeverity.EXPIRING_15_DAYS) {
                count15Days++;
            } else if (alert.getSeverity() == ExpirySeverity.EXPIRING_30_DAYS) {
                count30Days++;
            }
        }

        this.totalLowStockProducts = 0;
        this.totalOutOfStockProducts = 0;

        for (LowStockAlert alert : lowStockAlerts) {
            if (alert.isOutOfStock()) {
                totalOutOfStockProducts++;
            } else {
                totalLowStockProducts++;
            }
        }
    }

    public List<ExpiryAlert> getExpiredAlerts() {
        return expiredAlerts;
    }

    public List<ExpiryAlert> getExpiringSoonAlerts() {
        return expiringSoonAlerts;
    }

    public List<LowStockAlert> getLowStockAlerts() {
        return lowStockAlerts;
    }

    public int getTotalExpiredBatches() {
        return totalExpiredBatches;
    }

    public int getTotalExpiringSoonBatches() {
        return totalExpiringSoonBatches;
    }

    public int getCount7Days() {
        return count7Days;
    }

    public int getCount15Days() {
        return count15Days;
    }

    public int getCount30Days() {
        return count30Days;
    }

    public int getTotalLowStockProducts() {
        return totalLowStockProducts;
    }

    public int getTotalOutOfStockProducts() {
        return totalOutOfStockProducts;
    }

    public int getTotalActiveAlerts() {
        return totalExpiredBatches + totalExpiringSoonBatches + totalLowStockProducts + totalOutOfStockProducts;
    }
}
