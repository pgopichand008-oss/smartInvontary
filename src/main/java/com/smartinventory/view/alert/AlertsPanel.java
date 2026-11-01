package com.smartinventory.view.alert;

import com.smartinventory.controller.AlertController;
import com.smartinventory.controller.CategoryController;
import com.smartinventory.model.AlertSummary;
import com.smartinventory.model.Category;
import com.smartinventory.model.ExpiryAlert;
import com.smartinventory.model.LowStockAlert;
import com.smartinventory.model.enums.ExpirySeverity;
import com.smartinventory.view.components.AlertBadgeRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Surveillance and Alert Center for Expiration Tracking and Stock Deficits (Phase 6).
 */
public class AlertsPanel extends JPanel {

    private final AlertController alertController;
    private final CategoryController categoryController;

    // KPI Metric Tile Labels
    private JLabel expiredCountLabel;
    private JLabel expiringSoonCountLabel;
    private JLabel outOfStockCountLabel;
    private JLabel lowStockCountLabel;

    // Filter controls
    private JComboBox<CategoryFilterItem> categoryFilterCombo;
    private JComboBox<String> horizonFilterCombo;
    private JTextField searchField;
    private JLabel statusLabel;

    // Tab 1: Expiring Soon Table
    private JTable expiringTable;
    private DefaultTableModel expiringTableModel;

    // Tab 2: Expired Table
    private JTable expiredTable;
    private DefaultTableModel expiredTableModel;

    // Tab 3: Low Stock Table
    private JTable lowStockTable;
    private DefaultTableModel lowStockTableModel;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public AlertsPanel() {
        this(new AlertController(), new CategoryController());
    }

    public AlertsPanel(AlertController alertController, CategoryController categoryController) {
        this.alertController = alertController;
        this.categoryController = categoryController;

        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        loadCategories();
        refreshAlerts();
    }

    private void initComponents() {
        // --- 1. Top Section: Header & KPI Cards ---
        JPanel topContainer = new JPanel(new BorderLayout(12, 12));

        // Header Title
        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Expiry Date Engine & Low-Stock Alert Center");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitleLabel = new JLabel("Live automated surveillance of expired inventory, upcoming expirations (7, 15, 30 days), and stock shortages.");
        subtitleLabel.setForeground(Color.GRAY);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(subtitleLabel);
        topContainer.add(headerTextPanel, BorderLayout.NORTH);

        // KPI Metric Cards Grid
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiGrid.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        kpiGrid.add(createKpiCard("Expired Lots", "Immediate quarantine needed",
                new Color(254, 226, 226), new Color(185, 28, 28),
                expiredCountLabel = new JLabel("0")));

        kpiGrid.add(createKpiCard("Expiring Soon (≤30d)", "7d / 15d / 30d horizons",
                new Color(254, 240, 138), new Color(133, 77, 14),
                expiringSoonCountLabel = new JLabel("0")));

        kpiGrid.add(createKpiCard("Out of Stock", "0 units eligible stock",
                new Color(255, 228, 230), new Color(190, 18, 60),
                outOfStockCountLabel = new JLabel("0")));

        kpiGrid.add(createKpiCard("Low Stock Deficit", "Below minimum stock level",
                new Color(254, 243, 199), new Color(180, 83, 9),
                lowStockCountLabel = new JLabel("0")));

        topContainer.add(kpiGrid, BorderLayout.CENTER);

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));
        categoryFilterCombo.addActionListener(e -> onFilter());

        horizonFilterCombo = new JComboBox<>(new String[]{
                "Expiring Within 30 Days",
                "Critical (Within 7 Days)",
                "Urgent (Within 15 Days)"
        });
        horizonFilterCombo.addActionListener(e -> onFilter());

        searchField = new JTextField(16);
        searchField.putClientProperty("JTextField.placeholderText", "Search Product, Batch #, SKU...");
        searchField.addActionListener(e -> onFilter());

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> onFilter());

        JButton refreshBtn = new JButton("Refresh Alerts");
        refreshBtn.putClientProperty("JButton.buttonType", "roundRect");
        refreshBtn.setBackground(new Color(37, 99, 235));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilterCombo.setSelectedIndex(0);
            horizonFilterCombo.setSelectedIndex(0);
            refreshAlerts();
        });

        filterBar.add(new JLabel("Category:"));
        filterBar.add(categoryFilterCombo);
        filterBar.add(new JLabel("Horizon:"));
        filterBar.add(horizonFilterCombo);
        filterBar.add(searchField);
        filterBar.add(searchBtn);
        filterBar.add(refreshBtn);

        topContainer.add(filterBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // --- 2. Center: Tabbed Alert Tables ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 13f));

        tabbedPane.addTab("  ⚠️ Upcoming Expirations  ", createExpiringTablePanel());
        tabbedPane.addTab("  🚫 Expired Lots  ", createExpiredTablePanel());
        tabbedPane.addTab("  📉 Low & Out of Stock  ", createLowStockTablePanel());

        add(tabbedPane, BorderLayout.CENTER);

        // --- 3. Bottom: Status Bar ---
        JPanel bottomBar = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Surveillance engine active. Initializing...");
        statusLabel.setForeground(Color.GRAY);
        bottomBar.add(statusLabel, BorderLayout.WEST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createKpiCard(String title, String subtitle, Color bgColor, Color fgColor, JLabel countLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fgColor.brighter(), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        titleLbl.setForeground(fgColor);

        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 24f));
        countLabel.setForeground(fgColor);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 10f));
        subLbl.setForeground(fgColor.darker());

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(countLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createExpiringTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Batch ID", "Batch #", "Product Name", "SKU", "Category", "Supplier", "In Stock", "Expiry Date", "Days Remaining", "Urgency"};
        expiringTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        expiringTable = new JTable(expiringTableModel);
        setupTableProperties(expiringTable);
        expiringTable.getColumnModel().getColumn(9).setCellRenderer(new AlertBadgeRenderer());

        panel.add(new JScrollPane(expiringTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createExpiredTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Batch ID", "Batch #", "Product Name", "SKU", "Category", "Supplier", "Current Qty", "Expiry Date", "Expired Days Ago", "Status"};
        expiredTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        expiredTable = new JTable(expiredTableModel);
        setupTableProperties(expiredTable);
        expiredTable.getColumnModel().getColumn(9).setCellRenderer(new AlertBadgeRenderer());

        panel.add(new JScrollPane(expiredTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLowStockTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Product ID", "SKU", "Product Name", "Category", "Available Stock", "Min Stock Level", "Deficit", "Unit", "Severity"};
        lowStockTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        lowStockTable = new JTable(lowStockTableModel);
        setupTableProperties(lowStockTable);
        lowStockTable.getColumnModel().getColumn(8).setCellRenderer(new AlertBadgeRenderer());

        panel.add(new JScrollPane(lowStockTable), BorderLayout.CENTER);
        return panel;
    }

    private void setupTableProperties(JTable table) {
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));
    }

    private void loadCategories() {
        categoryController.loadCategories(
                categories -> {
                    categoryFilterCombo.removeAllItems();
                    categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));
                    for (Category c : categories) {
                        categoryFilterCombo.addItem(new CategoryFilterItem(c.getCategoryId(), c.getCategoryName()));
                    }
                },
                err -> {}
        );
    }

    public void refreshAlerts() {
        statusLabel.setText("Scanning inventory lots and stock levels...");
        CategoryFilterItem cat = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
        Integer catId = cat != null ? cat.getCategoryId() : null;
        String keyword = searchField.getText().trim();

        alertController.loadAlertSummary(catId, keyword,
                this::populateAlerts,
                err -> statusLabel.setText("Alert scanning error: " + err)
        );
    }

    private void onFilter() {
        refreshAlerts();
    }

    private void populateAlerts(AlertSummary summary) {
        // Update KPI Cards
        expiredCountLabel.setText(String.valueOf(summary.getTotalExpiredBatches()));
        expiringSoonCountLabel.setText(summary.getTotalExpiringSoonBatches() + " (7d: " + summary.getCount7Days() + ")");
        outOfStockCountLabel.setText(String.valueOf(summary.getTotalOutOfStockProducts()));
        lowStockCountLabel.setText(String.valueOf(summary.getTotalLowStockProducts()));

        // Horizon filter selection: 0 -> 30d, 1 -> 7d, 2 -> 15d
        int horizonDays = 30;
        int selectedIndex = horizonFilterCombo.getSelectedIndex();
        if (selectedIndex == 1) horizonDays = 7;
        else if (selectedIndex == 2) horizonDays = 15;

        // 1. Expiring Soon Table
        expiringTableModel.setRowCount(0);
        for (ExpiryAlert a : summary.getExpiringSoonAlerts()) {
            if (a.getDaysRemaining() <= horizonDays) {
                expiringTableModel.addRow(new Object[]{
                        a.getBatchId(),
                        a.getBatchNumber(),
                        a.getProductName(),
                        a.getSku(),
                        a.getCategoryName(),
                        a.getSupplierName(),
                        a.getCurrentQuantity() + " " + a.getUnit(),
                        a.getExpiryDate().toString(),
                        a.getFormattedDaysRemaining(),
                        a.getSeverity()
                });
            }
        }

        // 2. Expired Table
        expiredTableModel.setRowCount(0);
        for (ExpiryAlert a : summary.getExpiredAlerts()) {
            expiredTableModel.addRow(new Object[]{
                    a.getBatchId(),
                    a.getBatchNumber(),
                    a.getProductName(),
                    a.getSku(),
                    a.getCategoryName(),
                    a.getSupplierName(),
                    a.getCurrentQuantity() + " " + a.getUnit(),
                    a.getExpiryDate().toString(),
                    a.getFormattedDaysRemaining(),
                    a.getSeverity()
            });
        }

        // 3. Low Stock Table
        lowStockTableModel.setRowCount(0);
        for (LowStockAlert a : summary.getLowStockAlerts()) {
            lowStockTableModel.addRow(new Object[]{
                    a.getProductId(),
                    a.getSku(),
                    a.getProductName(),
                    a.getCategoryName(),
                    a.getCurrentStock(),
                    a.getMinStockLevel(),
                    "-" + a.getDeficit(),
                    a.getUnit(),
                    a.getSeverity()
            });
        }

        statusLabel.setText("Last refreshed at " + timeFormat.format(new Date()) +
                " | Found " + summary.getTotalExpiredBatches() + " expired lot(s), " +
                summary.getTotalExpiringSoonBatches() + " expiring soon, " +
                summary.getTotalOutOfStockProducts() + " out of stock, " +
                summary.getTotalLowStockProducts() + " low stock.");
    }

    private static class CategoryFilterItem {
        private final Integer categoryId;
        private final String categoryName;

        public CategoryFilterItem(Integer categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        public Integer getCategoryId() {
            return categoryId;
        }

        @Override
        public String toString() {
            return categoryName;
        }
    }
}
