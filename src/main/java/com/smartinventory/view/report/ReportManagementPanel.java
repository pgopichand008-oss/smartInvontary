package com.smartinventory.view.report;

import com.smartinventory.controller.CategoryController;
import com.smartinventory.controller.ReportController;
import com.smartinventory.model.Category;
import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.model.report.ReportData;
import com.smartinventory.model.report.ReportType;
import com.smartinventory.util.UserSession;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Modern Reporting & Analytics View supporting interactive report generation,
 * tabular data preview, and pure-Java PDF, Excel (.xls), and CSV exports.
 */
public class ReportManagementPanel extends JPanel {

    private final ReportController reportController;
    private final CategoryController categoryController;

    // Filter controls
    private JComboBox<ReportType> reportTypeCombo;
    private JLabel reportDescLabel;
    private JComboBox<CategoryFilterItem> categoryFilterCombo;
    private JComboBox<HorizonItem> horizonFilterCombo;
    private JComboBox<TransactionTypeItem> txTypeFilterCombo;
    private JComboBox<Integer> limitCombo;

    private JButton generateBtn;
    private JButton exportPdfBtn;
    private JButton exportExcelBtn;
    private JButton exportCsvBtn;
    private JLabel statusLabel;

    // Table preview
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private JPanel summaryNotesPanel;
    private JLabel summaryTextLabel;

    // Active state
    private ReportData currentReportData;

    public ReportManagementPanel() {
        this(new ReportController(), new CategoryController());
    }

    public ReportManagementPanel(ReportController reportController, CategoryController categoryController) {
        this.reportController = reportController;
        this.categoryController = categoryController;

        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        loadCategories();
        generateSelectedReport();
    }

    private void initComponents() {
        // --- 1. Top Section: Header & Control Bar ---
        JPanel topContainer = new JPanel(new BorderLayout(12, 12));

        // Header Title
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Reporting & Analytics Center");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subtitleLabel = new JLabel("Generate, preview, and export inventory valuation, expiration surveillance, and transaction audit reports.");
        subtitleLabel.setForeground(new Color(100, 116, 139));
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        topContainer.add(titlePanel, BorderLayout.NORTH);

        // Control Card (Report selection & filters)
        JPanel controlCard = new JPanel(new BorderLayout(10, 10));
        controlCard.setBackground(Color.WHITE);
        controlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Row 1: Report Type Selector & Description
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        typeRow.setOpaque(false);

        JLabel selectLabel = new JLabel("Report Type:");
        selectLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        typeRow.add(selectLabel);

        reportTypeCombo = new JComboBox<>(ReportType.values());
        reportTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportTypeCombo.setPreferredSize(new Dimension(280, 30));
        reportTypeCombo.addActionListener(e -> onReportTypeChanged());
        typeRow.add(reportTypeCombo);

        reportDescLabel = new JLabel("Description text");
        reportDescLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        reportDescLabel.setForeground(new Color(100, 116, 139));
        typeRow.add(reportDescLabel);

        controlCard.add(typeRow, BorderLayout.NORTH);

        // Row 2: Dynamic Filters & Action Buttons
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterRow.setOpaque(false);

        // Category Filter
        filterRow.add(new JLabel("Category:"));
        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));
        filterRow.add(categoryFilterCombo);

        // Horizon Filter (for Expiry report)
        filterRow.add(new JLabel("Horizon:"));
        horizonFilterCombo = new JComboBox<>();
        horizonFilterCombo.addItem(new HorizonItem(30, "30 Days"));
        horizonFilterCombo.addItem(new HorizonItem(15, "15 Days"));
        horizonFilterCombo.addItem(new HorizonItem(7, "7 Days (Critical)"));
        horizonFilterCombo.addItem(new HorizonItem(60, "60 Days"));
        filterRow.add(horizonFilterCombo);

        // Transaction Type Filter (for Audit report)
        filterRow.add(new JLabel("Tx Type:"));
        txTypeFilterCombo = new JComboBox<>();
        txTypeFilterCombo.addItem(new TransactionTypeItem(null, "All Movement Types"));
        for (TransactionType tt : TransactionType.values()) {
            txTypeFilterCombo.addItem(new TransactionTypeItem(tt, tt.getDisplayName()));
        }
        filterRow.add(txTypeFilterCombo);

        // Limit
        filterRow.add(new JLabel("Max Rows:"));
        limitCombo = new JComboBox<>(new Integer[]{50, 100, 250, 500});
        filterRow.add(limitCombo);

        // Generate Button
        generateBtn = new JButton("Generate Preview");
        generateBtn.putClientProperty("JButton.buttonType", "roundRect");
        generateBtn.setBackground(new Color(37, 99, 235));
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        generateBtn.setFocusPainted(false);
        generateBtn.addActionListener(e -> generateSelectedReport());
        filterRow.add(generateBtn);

        controlCard.add(filterRow, BorderLayout.CENTER);

        // Row 3: Export Bar & Status
        JPanel exportBar = new JPanel(new BorderLayout(8, 0));
        exportBar.setOpaque(false);
        exportBar.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JPanel exportBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        exportBtns.setOpaque(false);

        JLabel exportLabel = new JLabel("Export Format:");
        exportLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportBtns.add(exportLabel);

        exportPdfBtn = createExportButton("Export PDF (.pdf)", new Color(220, 38, 38), () -> exportReport("pdf"));
        exportExcelBtn = createExportButton("Export Excel (.xls)", new Color(22, 101, 52), () -> exportReport("excel"));
        exportCsvBtn = createExportButton("Export CSV (.csv)", new Color(71, 85, 105), () -> exportReport("csv"));

        exportBtns.add(exportPdfBtn);
        exportBtns.add(exportExcelBtn);
        exportBtns.add(exportCsvBtn);

        exportBar.add(exportBtns, BorderLayout.WEST);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(100, 116, 139));
        exportBar.add(statusLabel, BorderLayout.EAST);

        controlCard.add(exportBar, BorderLayout.SOUTH);
        topContainer.add(controlCard, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // --- 2. Center Section: Data Preview Table ---
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        previewTable = new JTable(tableModel);
        previewTable.setRowHeight(28);
        previewTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        previewTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        previewTable.getTableHeader().setReorderingAllowed(false);
        previewTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Bottom Section: Summary Notes Panel ---
        summaryNotesPanel = new JPanel(new BorderLayout(8, 4));
        summaryNotesPanel.setBackground(new Color(248, 250, 252));
        summaryNotesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel summaryTitle = new JLabel("Report Summary & Analytics Notes:");
        summaryTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        summaryTitle.setForeground(new Color(15, 23, 42));
        summaryNotesPanel.add(summaryTitle, BorderLayout.NORTH);

        summaryTextLabel = new JLabel("No summary generated yet.");
        summaryTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        summaryTextLabel.setForeground(new Color(71, 85, 105));
        summaryNotesPanel.add(summaryTextLabel, BorderLayout.CENTER);

        add(summaryNotesPanel, BorderLayout.SOUTH);

        onReportTypeChanged();
    }

    private JButton createExportButton(String text, Color color, Runnable action) {
        JButton btn = new JButton(text);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void onReportTypeChanged() {
        ReportType selected = (ReportType) reportTypeCombo.getSelectedItem();
        if (selected == null) return;

        reportDescLabel.setText(selected.getDescription());

        // Toggle filter visibility based on report
        boolean isExpiry = selected == ReportType.EXPIRY_SURVEILLANCE;
        boolean isTx = selected == ReportType.TRANSACTION_AUDIT;

        horizonFilterCombo.setEnabled(isExpiry);
        txTypeFilterCombo.setEnabled(isTx);
        categoryFilterCombo.setEnabled(!isTx);
    }

    private void loadCategories() {
        categoryController.loadCategories(
                categories -> {
                    CategoryFilterItem selected = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
                    categoryFilterCombo.removeAllItems();
                    categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));
                    for (Category c : categories) {
                        categoryFilterCombo.addItem(new CategoryFilterItem(c.getCategoryId(), c.getCategoryName()));
                    }
                    if (selected != null && selected.id != null) {
                        for (int i = 0; i < categoryFilterCombo.getItemCount(); i++) {
                            if (selected.id.equals(categoryFilterCombo.getItemAt(i).id)) {
                                categoryFilterCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                },
                error -> statusLabel.setText("Failed to load categories: " + error)
        );
    }

    /**
     * Executes the background compilation of the selected report.
     */
    public void generateSelectedReport() {
        ReportType type = (ReportType) reportTypeCombo.getSelectedItem();
        if (type == null) return;

        String operator = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getUsername() : "Operator";
        CategoryFilterItem catItem = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
        Integer categoryId = catItem != null ? catItem.id : null;

        setLoading(true);
        statusLabel.setText("Compiling " + type.getTitle() + "...");

        switch (type) {
            case INVENTORY_VALUATION:
                reportController.loadInventoryValuationReport(categoryId, operator, this::displayReport, this::onReportError);
                break;
            case EXPIRY_SURVEILLANCE:
                HorizonItem hItem = (HorizonItem) horizonFilterCombo.getSelectedItem();
                int horizon = hItem != null ? hItem.days : 30;
                reportController.loadExpirySurveillanceReport(categoryId, horizon, operator, this::displayReport, this::onReportError);
                break;
            case LOW_STOCK_REORDER:
                reportController.loadLowStockReport(categoryId, operator, this::displayReport, this::onReportError);
                break;
            case TRANSACTION_AUDIT:
                TransactionTypeItem tItem = (TransactionTypeItem) txTypeFilterCombo.getSelectedItem();
                TransactionType tType = tItem != null ? tItem.type : null;
                Integer limit = (Integer) limitCombo.getSelectedItem();
                reportController.loadTransactionAuditReport(null, tType, limit != null ? limit : 100, operator, this::displayReport, this::onReportError);
                break;
        }
    }

    private void displayReport(ReportData report) {
        this.currentReportData = report;
        setLoading(false);

        // Update table columns and rows
        Vector<String> colNames = new Vector<>(report.getHeaders());
        Vector<Vector<Object>> data = new Vector<>();

        for (List<Object> row : report.getRows()) {
            data.add(new Vector<>(row));
        }

        tableModel.setDataVector(data, colNames);

        // Update summary notes
        if (report.getSummaryNotes().isEmpty()) {
            summaryTextLabel.setText("No specific metrics flagged.");
        } else {
            summaryTextLabel.setText("<html>" + String.join(" &nbsp;|&nbsp; ", report.getSummaryNotes()) + "</html>");
        }

        statusLabel.setText("Generated: " + report.getRows().size() + " records at " + report.getGeneratedAt());
    }

    private void onReportError(String errorMsg) {
        setLoading(false);
        statusLabel.setText("Compilation failed: " + errorMsg);
        JOptionPane.showMessageDialog(this,
                "Error generating report:\n" + errorMsg,
                "Report Error", JOptionPane.ERROR_MESSAGE);
    }

    private void exportReport(String format) {
        if (currentReportData == null || currentReportData.getRows().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No data to export. Please generate a report first.",
                    "Export Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String extension = format.toLowerCase().contains("pdf") ? "pdf" :
                (format.toLowerCase().contains("xls") ? "xls" : "csv");

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String defaultFileName = currentReportData.getReportType().name().toLowerCase()
                + "_" + df.format(new Date()) + "." + extension;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                extension.toUpperCase() + " Files (*." + extension + ")", extension));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File targetFile = fileChooser.getSelectedFile();
        if (!targetFile.getName().toLowerCase().endsWith("." + extension)) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + "." + extension);
        }

        if (targetFile.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "File '" + targetFile.getName() + "' already exists.\nDo you want to overwrite it?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        File finalTarget = targetFile;
        setLoading(true);
        statusLabel.setText("Exporting to " + extension.toUpperCase() + "...");

        reportController.exportReport(finalTarget, currentReportData, extension,
                () -> {
                    setLoading(false);
                    statusLabel.setText("Successfully exported: " + finalTarget.getName());
                    JOptionPane.showMessageDialog(this,
                            "Report successfully exported to:\n" + finalTarget.getAbsolutePath(),
                            "Export Success", JOptionPane.INFORMATION_MESSAGE);
                },
                error -> {
                    setLoading(false);
                    statusLabel.setText("Export failed: " + error);
                    JOptionPane.showMessageDialog(this,
                            "Failed to export report:\n" + error,
                            "Export Failed", JOptionPane.ERROR_MESSAGE);
                }
        );
    }

    private void setLoading(boolean loading) {
        generateBtn.setEnabled(!loading);
        exportPdfBtn.setEnabled(!loading);
        exportExcelBtn.setEnabled(!loading);
        exportCsvBtn.setEnabled(!loading);
        reportTypeCombo.setEnabled(!loading);
    }

    // Helper Filter Wrapper Records/Classes
    private static class CategoryFilterItem {
        final Integer id;
        final String name;

        CategoryFilterItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class HorizonItem {
        final int days;
        final String label;

        HorizonItem(int days, String label) {
            this.days = days;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class TransactionTypeItem {
        final TransactionType type;
        final String label;

        TransactionTypeItem(TransactionType type, String label) {
            this.type = type;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
