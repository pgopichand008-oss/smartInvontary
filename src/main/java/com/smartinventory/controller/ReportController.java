package com.smartinventory.controller;

import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.model.report.ReportData;
import com.smartinventory.service.ReportService;
import com.smartinventory.util.export.CsvReportExporter;
import com.smartinventory.util.export.ExcelReportExporter;
import com.smartinventory.util.export.PdfReportExporter;

import javax.swing.*;
import java.io.File;
import java.util.function.Consumer;

/**
 * Controller orchestrating background report compilation and file exports (PDF, Excel, CSV).
 */
public class ReportController {

    private final ReportService reportService;

    public ReportController() {
        this(new ReportService());
    }

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public void loadInventoryValuationReport(Integer categoryId, String operator,
                                            Consumer<ReportData> onSuccess, Consumer<String> onError) {
        SwingWorker<ReportData, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportData doInBackground() {
                return reportService.generateInventoryValuationReport(categoryId, operator);
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

    public void loadExpirySurveillanceReport(Integer categoryId, Integer maxDays, String operator,
                                            Consumer<ReportData> onSuccess, Consumer<String> onError) {
        SwingWorker<ReportData, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportData doInBackground() {
                return reportService.generateExpirySurveillanceReport(categoryId, maxDays, operator);
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

    public void loadLowStockReport(Integer categoryId, String operator,
                                   Consumer<ReportData> onSuccess, Consumer<String> onError) {
        SwingWorker<ReportData, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportData doInBackground() {
                return reportService.generateLowStockReport(categoryId, operator);
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

    public void loadTransactionAuditReport(Integer productId, TransactionType type, int limit, String operator,
                                           Consumer<ReportData> onSuccess, Consumer<String> onError) {
        SwingWorker<ReportData, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportData doInBackground() {
                return reportService.generateTransactionAuditReport(productId, type, limit, operator);
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
     * Asynchronously exports report data to PDF, Excel, or CSV.
     */
    public void exportReport(File file, ReportData report, String format,
                             Runnable onSuccess, Consumer<String> onError) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String fmt = format != null ? format.toLowerCase() : "pdf";
                if (fmt.contains("pdf")) {
                    PdfReportExporter.exportToFile(file, report);
                } else if (fmt.contains("xls") || fmt.contains("excel")) {
                    ExcelReportExporter.exportToFile(file, report);
                } else {
                    CsvReportExporter.exportToFile(file, report);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    onSuccess.run();
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
