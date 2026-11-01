package com.smartinventory.view.inventory;

import com.smartinventory.controller.InventoryController;
import com.smartinventory.controller.ProductController;
import com.smartinventory.controller.SupplierController;
import com.smartinventory.model.Batch;
import com.smartinventory.model.Product;
import com.smartinventory.model.StockTransaction;
import com.smartinventory.model.Supplier;
import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.view.components.InventoryStatusBadgeRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Management Panel for Phase 5:
 * Integrates Batch Lot Tracking, Stock-In, FEFO Stock-Out, and Immutable Movement Ledger.
 */
public class InventoryManagementPanel extends JPanel {

    private final InventoryController inventoryController;
    private final ProductController productController;
    private final SupplierController supplierController;

    private final List<Batch> currentBatches = new ArrayList<>();
    private final List<StockTransaction> currentTransactions = new ArrayList<>();

    // Tab 1 Components (Batches)
    private JTable batchTable;
    private DefaultTableModel batchTableModel;
    private JComboBox<FilterItem> productFilterCombo;
    private JComboBox<FilterItem> supplierFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JTextField batchSearchField;
    private JLabel batchStatusLabel;

    // Tab 2 Components (Ledger)
    private JTable transactionTable;
    private DefaultTableModel transactionTableModel;
    private JComboBox<String> txTypeFilterCombo;
    private JTextField txSearchField;
    private JLabel txStatusLabel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public InventoryManagementPanel() {
        this(new InventoryController(), new ProductController(), new SupplierController());
    }

    public InventoryManagementPanel(InventoryController inventoryController,
                                  ProductController productController,
                                  SupplierController supplierController) {
        this.inventoryController = inventoryController;
        this.productController = productController;
        this.supplierController = supplierController;

        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        loadDropdownFilters();
        loadBatches();
        loadTransactions();
    }

    private void initComponents() {
        // --- 1. Top Header ---
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));

        JLabel titleLabel = new JLabel("Inventory & Batch Operations");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitleLabel = new JLabel("Manage multi-lot batches, execute FEFO stock allocation, and inspect audit trails.");
        subtitleLabel.setForeground(Color.GRAY);

        headerTextPanel.add(titleLabel);
        headerTextPanel.add(subtitleLabel);
        topPanel.add(headerTextPanel, BorderLayout.WEST);

        // Header Action Buttons: Stock In & Stock Out
        JPanel primaryActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton stockInBtn = new JButton("+ Stock In (Receive)");
        stockInBtn.putClientProperty("JButton.buttonType", "roundRect");
        stockInBtn.setBackground(new Color(37, 99, 235));
        stockInBtn.setForeground(Color.WHITE);
        stockInBtn.setFont(stockInBtn.getFont().deriveFont(Font.BOLD));
        stockInBtn.addActionListener(e -> onStockIn());

        JButton stockOutBtn = new JButton("- Stock Out (FEFO)");
        stockOutBtn.putClientProperty("JButton.buttonType", "roundRect");
        stockOutBtn.setBackground(new Color(13, 148, 136));
        stockOutBtn.setForeground(Color.WHITE);
        stockOutBtn.setFont(stockOutBtn.getFont().deriveFont(Font.BOLD));
        stockOutBtn.addActionListener(e -> onStockOut());

        primaryActionPanel.add(stockInBtn);
        primaryActionPanel.add(stockOutBtn);
        topPanel.add(primaryActionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- 2. Tabbed Center Pane ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 13f));

        tabbedPane.addTab("  Batch Lots & Inventory  ", createBatchTabPanel());
        tabbedPane.addTab("  Stock Movement Ledger  ", createLedgerTabPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createBatchTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        productFilterCombo = new JComboBox<>();
        productFilterCombo.addItem(new FilterItem(null, "All Products"));
        productFilterCombo.addActionListener(e -> onFilterBatches());

        supplierFilterCombo = new JComboBox<>();
        supplierFilterCombo.addItem(new FilterItem(null, "All Suppliers"));
        supplierFilterCombo.addActionListener(e -> onFilterBatches());

        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "ACTIVE", "EXPIRING_SOON", "EXPIRED", "DEPLETED"});
        statusFilterCombo.addActionListener(e -> onFilterBatches());

        batchSearchField = new JTextField(14);
        batchSearchField.putClientProperty("JTextField.placeholderText", "Search Batch #, Product, SKU...");
        batchSearchField.addActionListener(e -> onFilterBatches());

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> onFilterBatches());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            batchSearchField.setText("");
            productFilterCombo.setSelectedIndex(0);
            supplierFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            loadBatches();
        });

        filterBar.add(new JLabel("Product:"));
        filterBar.add(productFilterCombo);
        filterBar.add(new JLabel("Supplier:"));
        filterBar.add(supplierFilterCombo);
        filterBar.add(new JLabel("Status:"));
        filterBar.add(statusFilterCombo);
        filterBar.add(batchSearchField);
        filterBar.add(searchBtn);
        filterBar.add(refreshBtn);
        panel.add(filterBar, BorderLayout.NORTH);

        // Batch Table
        String[] columns = {"ID", "Batch Number", "Product Name", "SKU", "Supplier", "In Stock", "Initial Qty", "Expiry Date", "Status", "Cost ($)", "Sell ($)"};
        batchTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        batchTable = new JTable(batchTableModel);
        batchTable.setRowHeight(32);
        batchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        batchTable.setShowGrid(true);
        batchTable.setGridColor(new Color(241, 245, 249));

        // Column widths
        batchTable.getColumnModel().getColumn(0).setPreferredWidth(45);   // ID
        batchTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Batch #
        batchTable.getColumnModel().getColumn(2).setPreferredWidth(180);  // Product Name
        batchTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // SKU
        batchTable.getColumnModel().getColumn(4).setPreferredWidth(140);  // Supplier
        batchTable.getColumnModel().getColumn(5).setPreferredWidth(80);   // In Stock
        batchTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Initial Qty
        batchTable.getColumnModel().getColumn(7).setPreferredWidth(95);   // Expiry Date
        batchTable.getColumnModel().getColumn(8).setPreferredWidth(110);  // Status
        batchTable.getColumnModel().getColumn(9).setPreferredWidth(75);   // Cost
        batchTable.getColumnModel().getColumn(10).setPreferredWidth(75);  // Sell

        // Status Badge Renderer for Column 8
        batchTable.getColumnModel().getColumn(8).setCellRenderer(new InventoryStatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(batchTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom status summary
        JPanel bottomBar = new JPanel(new BorderLayout());
        batchStatusLabel = new JLabel("Loading batches...");
        batchStatusLabel.setForeground(Color.GRAY);
        bottomBar.add(batchStatusLabel, BorderLayout.WEST);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createLedgerTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        txTypeFilterCombo = new JComboBox<>(new String[]{
                "All Transaction Types",
                "PURCHASE_IN",
                "SALE_OUT",
                "RETURN_IN",
                "DAMAGE_OUT",
                "EXPIRED_WRITE_OFF"
        });
        txTypeFilterCombo.addActionListener(e -> onFilterTransactions());

        txSearchField = new JTextField(18);
        txSearchField.putClientProperty("JTextField.placeholderText", "Search Product, Batch, Note, User...");
        txSearchField.addActionListener(e -> onFilterTransactions());

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> onFilterTransactions());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            txSearchField.setText("");
            txTypeFilterCombo.setSelectedIndex(0);
            loadTransactions();
        });

        filterBar.add(new JLabel("Movement Type:"));
        filterBar.add(txTypeFilterCombo);
        filterBar.add(txSearchField);
        filterBar.add(searchBtn);
        filterBar.add(refreshBtn);
        panel.add(filterBar, BorderLayout.NORTH);

        // Transaction Table
        String[] columns = {"Tx ID", "Date & Time", "Movement Type", "Product Name", "Batch #", "Quantity", "Balance After", "Operator", "Reference Note"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(transactionTableModel);
        transactionTable.setRowHeight(32);
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setShowGrid(true);
        transactionTable.setGridColor(new Color(241, 245, 249));

        // Column widths
        transactionTable.getColumnModel().getColumn(0).setPreferredWidth(55);   // Tx ID
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(125);  // Date
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Type
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(180);  // Product Name
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(110);  // Batch #
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(70);   // Quantity
        transactionTable.getColumnModel().getColumn(6).setPreferredWidth(85);   // Balance After
        transactionTable.getColumnModel().getColumn(7).setPreferredWidth(90);   // Operator
        transactionTable.getColumnModel().getColumn(8).setPreferredWidth(200);  // Note

        // Badge renderer on Movement Type
        transactionTable.getColumnModel().getColumn(2).setCellRenderer(new InventoryStatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom status summary
        JPanel bottomBar = new JPanel(new BorderLayout());
        txStatusLabel = new JLabel("Loading transaction ledger...");
        txStatusLabel.setForeground(Color.GRAY);
        bottomBar.add(txStatusLabel, BorderLayout.WEST);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadDropdownFilters() {
        productController.loadProducts(null, null,
                products -> {
                    productFilterCombo.removeAllItems();
                    productFilterCombo.addItem(new FilterItem(null, "All Products"));
                    for (Product p : products) {
                        productFilterCombo.addItem(new FilterItem(p.getProductId(), p.getProductName()));
                    }
                },
                err -> {}
        );

        supplierController.loadSuppliers(null,
                suppliers -> {
                    supplierFilterCombo.removeAllItems();
                    supplierFilterCombo.addItem(new FilterItem(null, "All Suppliers"));
                    for (Supplier s : suppliers) {
                        supplierFilterCombo.addItem(new FilterItem(s.getSupplierId(), s.getSupplierName()));
                    }
                },
                err -> {}
        );
    }

    private void loadBatches() {
        batchStatusLabel.setText("Loading batches...");
        FilterItem prodItem = (FilterItem) productFilterCombo.getSelectedItem();
        FilterItem suppItem = (FilterItem) supplierFilterCombo.getSelectedItem();
        BatchStatus status = getSelectedStatus();

        inventoryController.loadBatches(
                prodItem != null ? prodItem.getId() : null,
                suppItem != null ? suppItem.getId() : null,
                status,
                this::populateBatchTable,
                error -> batchStatusLabel.setText("Error loading batches: " + error)
        );
    }

    private void onFilterBatches() {
        String keyword = batchSearchField.getText().trim();
        FilterItem prodItem = (FilterItem) productFilterCombo.getSelectedItem();
        FilterItem suppItem = (FilterItem) supplierFilterCombo.getSelectedItem();
        BatchStatus status = getSelectedStatus();

        inventoryController.searchBatches(
                keyword,
                prodItem != null ? prodItem.getId() : null,
                suppItem != null ? suppItem.getId() : null,
                status,
                this::populateBatchTable,
                error -> batchStatusLabel.setText("Error searching batches: " + error)
        );
    }

    private BatchStatus getSelectedStatus() {
        int index = statusFilterCombo.getSelectedIndex();
        if (index <= 0) return null;
        try {
            return BatchStatus.valueOf((String) statusFilterCombo.getSelectedItem());
        } catch (Exception e) {
            return null;
        }
    }

    private void populateBatchTable(List<Batch> batches) {
        currentBatches.clear();
        currentBatches.addAll(batches);

        batchTableModel.setRowCount(0);
        int totalUnits = 0;

        for (Batch b : batches) {
            totalUnits += b.getCurrentQuantity();
            batchTableModel.addRow(new Object[]{
                    b.getBatchId(),
                    b.getBatchNumber(),
                    b.getProductName(),
                    b.getProductSku(),
                    b.getSupplierName(),
                    b.getCurrentQuantity() + " " + (b.getProductUnit() != null ? b.getProductUnit() : "pcs"),
                    b.getInitialQuantity(),
                    b.getExpiryDate() != null ? b.getExpiryDate().toString() : "-",
                    b.getStatus(),
                    String.format("%.2f", b.getPurchasePrice()),
                    String.format("%.2f", b.getSellingPrice())
            });
        }
        batchStatusLabel.setText("Showing " + batches.size() + " batch(es) | Total Available Quantity in View: " + totalUnits);
    }

    private void loadTransactions() {
        txStatusLabel.setText("Loading transaction ledger...");
        inventoryController.loadTransactions(100, this::populateTransactionTable,
                error -> txStatusLabel.setText("Error loading transactions: " + error)
        );
    }

    private void onFilterTransactions() {
        String keyword = txSearchField.getText().trim();
        TransactionType type = null;
        if (txTypeFilterCombo.getSelectedIndex() > 0) {
            type = TransactionType.valueOf((String) txTypeFilterCombo.getSelectedItem());
        }

        inventoryController.searchTransactions(keyword, type, null, 100, this::populateTransactionTable,
                error -> txStatusLabel.setText("Error searching transactions: " + error)
        );
    }

    private void populateTransactionTable(List<StockTransaction> transactions) {
        currentTransactions.clear();
        currentTransactions.addAll(transactions);

        transactionTableModel.setRowCount(0);
        for (StockTransaction t : transactions) {
            String dateStr = t.getCreatedAt() != null ? dateFormat.format(t.getCreatedAt()) : "-";
            transactionTableModel.addRow(new Object[]{
                    t.getTransactionId(),
                    dateStr,
                    t.getTransactionType(),
                    t.getProductName(),
                    t.getBatchNumber(),
                    (t.getTransactionType().isIncoming() ? "+" : "-") + t.getQuantity(),
                    t.getBalanceAfter(),
                    t.getUsername() != null ? t.getUsername() : "User #" + t.getUserId(),
                    t.getReferenceNote() != null ? t.getReferenceNote() : "-"
            });
        }
        txStatusLabel.setText("Showing latest " + transactions.size() + " transaction record(s).");
    }

    private void onStockIn() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        StockInDialog dialog = new StockInDialog(parent, inventoryController, productController, supplierController);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            loadBatches();
            loadTransactions();
        }
    }

    private void onStockOut() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        StockOutDialog dialog = new StockOutDialog(parent, inventoryController, productController);
        dialog.setVisible(true);

        if (dialog.isCompleted()) {
            loadBatches();
            loadTransactions();
        }
    }

    private static class FilterItem {
        private final Integer id;
        private final String name;

        public FilterItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
