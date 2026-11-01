package com.smartinventory.model.dashboard;

/**
 * Monthly aggregate stock movement statistics (Purchases vs Sales vs Discards).
 */
public class MonthlyMovementStat {

    private final String monthPeriod; // YYYY-MM
    private final int totalInflow;     // PURCHASE_IN + RETURN_IN
    private final int totalOutflow;    // SALE_OUT
    private final int totalDiscard;    // DAMAGE_OUT + EXPIRED_WRITE_OFF

    public MonthlyMovementStat(String monthPeriod, int totalInflow, int totalOutflow, int totalDiscard) {
        this.monthPeriod = monthPeriod;
        this.totalInflow = totalInflow;
        this.totalOutflow = totalOutflow;
        this.totalDiscard = totalDiscard;
    }

    public String getMonthPeriod() {
        return monthPeriod;
    }

    public String getMonth() {
        return monthPeriod;
    }

    public int getTotalInflow() {
        return totalInflow;
    }

    public int getInflow() {
        return totalInflow;
    }

    public int getTotalOutflow() {
        return totalOutflow;
    }

    public int getOutflow() {
        return totalOutflow;
    }

    public int getTotalDiscard() {
        return totalDiscard;
    }

    public int getDiscards() {
        return totalDiscard;
    }
}
