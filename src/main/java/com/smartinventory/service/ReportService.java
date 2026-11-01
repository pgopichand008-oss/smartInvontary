package com.smartinventory.service;

import com.smartinventory.dao.AlertDAO;
import com.smartinventory.dao.BatchDAO;
import com.smartinventory.dao.StockTransactionDAO;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.model.Batch;
import com.smartinventory.model.ExpiryAlert;
import com.smartinventory.model.LowStockAlert;
import com.smartinventory.model.StockTransaction;
import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.model.report.ReportData;
import com.smartinventory.model.report.ReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service Layer for compiling comprehensive inventory, expiration, deficit,
 * and transaction audit reports.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final BatchDAO batchDAO;
    private final AlertDAO alertDAO;
    private final StockTransactionDAO transactionDAO;

    public ReportService() {
        this(new BatchDAO(), new AlertDAO(), new StockTransactionDAO());
    }

    public ReportService(BatchDAO batchDAO, AlertDAO alertDAO, StockTransactionDAO transactionDAO) {
        this.batchDAO = batchDAO;
        this.alertDAO = alertDAO;
        this.transactionDAO = transactionDAO;
    }

    /**
     * Compiles an Inventory Valuation & Stock Summary Report.
     */
    public ReportData generateInventoryValuationReport(Integer categoryId, String operatorName) {
        try {
            List<Batch> batches = batchDAO.search(null, null, null, null, categoryId);
            List<String> headers = Arrays.asList(
                    "Batch #", "Product Name", "SKU", "Category", "Supplier",
                    "In Stock", "Unit", "Cost ($)", "Sell ($)", "Total Value ($)", "Expiry Date", "Status"
            );

            List<List<Object>> rows = new ArrayList<>();
            int totalUnits = 0;
            BigDecimal totalValuation = BigDecimal.ZERO;

            for (Batch b : batches) {
                BigDecimal batchValue = b.getPurchasePrice().multiply(BigDecimal.valueOf(b.getCurrentQuantity()));
                totalUnits += b.getCurrentQuantity();
                totalValuation = totalValuation.add(batchValue);

                rows.add(Arrays.asList(
                        b.getBatchNumber(),
                        b.getProductName(),
                        b.getProductSku(),
                        b.getCategoryName() != null ? b.getCategoryName() : "-",
                        b.getSupplierName(),
                        b.getCurrentQuantity(),
                        b.getProductUnit() != null ? b.getProductUnit() : "pcs",
                        String.format("%.2f", b.getPurchasePrice()),
                        String.format("%.2f", b.getSellingPrice()),
                        String.format("%.2f", batchValue),
                        b.getExpiryDate() != null ? b.getExpiryDate().toString() : "-",
                        b.getStatus().getDisplayName()
                ));
            }

            List<String> summaryNotes = Arrays.asList(
                    "Total Batch Lots: " + batches.size(),
                    "Total Physical Units: " + totalUnits,
                    "Total Inventory Cost Valuation: $" + String.format("%.2f", totalValuation)
            );

            return new ReportData(ReportType.INVENTORY_VALUATION,
                    ReportType.INVENTORY_VALUATION.getTitle(),
                    operatorName, headers, rows, summaryNotes);
        } catch (SQLException e) {
            logger.error("Failed to generate inventory valuation report: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to compile valuation report: " + e.getMessage(), e);
        }
    }

    /**
     * Compiles an Expiry Surveillance Report.
     */
    public ReportData generateExpirySurveillanceReport(Integer categoryId, Integer maxDays, String operatorName) {
        try {
            int horizon = (maxDays != null && maxDays > 0) ? maxDays : 30;
            List<ExpiryAlert> expired = alertDAO.findExpiredBatches(categoryId, null);
            List<ExpiryAlert> expiringSoon = alertDAO.findExpiringSoonBatches(horizon, categoryId, null);

            List<String> headers = Arrays.asList(
                    "Batch #", "Product Name", "SKU", "Category", "Supplier",
                    "Current Qty", "Expiry Date", "Timeline", "Severity"
            );

            List<List<Object>> rows = new ArrayList<>();
            for (ExpiryAlert e : expired) {
                rows.add(Arrays.asList(
                        e.getBatchNumber(), e.getProductName(), e.getSku(),
                        e.getCategoryName(), e.getSupplierName(),
                        e.getCurrentQuantity() + " " + e.getUnit(),
                        e.getExpiryDate().toString(),
                        e.getFormattedDaysRemaining(),
                        "EXPIRED (Quarantine)"
                ));
            }

            for (ExpiryAlert e : expiringSoon) {
                rows.add(Arrays.asList(
                        e.getBatchNumber(), e.getProductName(), e.getSku(),
                        e.getCategoryName(), e.getSupplierName(),
                        e.getCurrentQuantity() + " " + e.getUnit(),
                        e.getExpiryDate().toString(),
                        e.getFormattedDaysRemaining(),
                        e.getSeverity().getDisplayName()
                ));
            }

            List<String> summaryNotes = Arrays.asList(
                    "Total Expired Lots Requiring Action: " + expired.size(),
                    "Total Upcoming Lots (≤ " + horizon + " days): " + expiringSoon.size()
            );

            return new ReportData(ReportType.EXPIRY_SURVEILLANCE,
                    ReportType.EXPIRY_SURVEILLANCE.getTitle(),
                    operatorName, headers, rows, summaryNotes);
        } catch (SQLException e) {
            logger.error("Failed to generate expiry surveillance report: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to compile expiry report: " + e.getMessage(), e);
        }
    }

    /**
     * Compiles a Low Stock & Reorder Deficit Report.
     */
    public ReportData generateLowStockReport(Integer categoryId, String operatorName) {
        try {
            List<LowStockAlert> alerts = alertDAO.findLowStockAlerts(categoryId, null);
            List<String> headers = Arrays.asList(
                    "Product ID", "SKU", "Product Name", "Category",
                    "Available Stock", "Safety Level", "Deficit", "Unit", "Severity"
            );

            List<List<Object>> rows = new ArrayList<>();
            int totalDeficit = 0;

            for (LowStockAlert a : alerts) {
                totalDeficit += a.getDeficit();
                rows.add(Arrays.asList(
                        a.getProductId(),
                        a.getSku(),
                        a.getProductName(),
                        a.getCategoryName(),
                        a.getCurrentStock(),
                        a.getMinStockLevel(),
                        "-" + a.getDeficit(),
                        a.getUnit(),
                        a.getSeverity().getDisplayName()
                ));
            }

            List<String> summaryNotes = Arrays.asList(
                    "Total Items Requiring Procurement: " + alerts.size(),
                    "Total Cumulative Deficit Units: " + totalDeficit
            );

            return new ReportData(ReportType.LOW_STOCK_REORDER,
                    ReportType.LOW_STOCK_REORDER.getTitle(),
                    operatorName, headers, rows, summaryNotes);
        } catch (SQLException e) {
            logger.error("Failed to generate low stock report: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to compile low stock report: " + e.getMessage(), e);
        }
    }

    /**
     * Compiles a Stock Movement Audit Ledger Report.
     */
    public ReportData generateTransactionAuditReport(Integer productId, TransactionType type, int limit, String operatorName) {
        try {
            List<StockTransaction> txList = transactionDAO.search(null, type, productId, Math.max(10, limit));
            List<String> headers = Arrays.asList(
                    "Tx ID", "Timestamp", "Movement Type", "Product Name", "SKU",
                    "Batch #", "Quantity", "Balance After", "Operator", "Reference Note"
            );

            List<List<Object>> rows = new ArrayList<>();
            for (StockTransaction t : txList) {
                rows.add(Arrays.asList(
                        t.getTransactionId(),
                        t.getCreatedAt() != null ? t.getCreatedAt().toString() : "-",
                        t.getTransactionType().getDisplayName(),
                        t.getProductName(),
                        t.getProductSku(),
                        t.getBatchNumber(),
                        (t.getTransactionType().isIncoming() ? "+" : "-") + t.getQuantity(),
                        t.getBalanceAfter(),
                        t.getUsername() != null ? t.getUsername() : "User #" + t.getUserId(),
                        t.getReferenceNote() != null ? t.getReferenceNote() : "-"
                ));
            }

            List<String> summaryNotes = Arrays.asList(
                    "Total Logged Transactions in Report: " + txList.size()
            );

            return new ReportData(ReportType.TRANSACTION_AUDIT,
                    ReportType.TRANSACTION_AUDIT.getTitle(),
                    operatorName, headers, rows, summaryNotes);
        } catch (SQLException e) {
            logger.error("Failed to generate transaction audit report: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to compile audit report: " + e.getMessage(), e);
        }
    }
}
