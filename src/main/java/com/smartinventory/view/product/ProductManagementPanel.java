package com.smartinventory.view.product;

import com.smartinventory.controller.CategoryController;
import com.smartinventory.controller.ProductController;
import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import com.smartinventory.util.UserSession;
import com.smartinventory.view.components.StatusBadgeRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Management Panel for the Product Catalog:
 * Provides search, category filtering, status filtering, table display with badges,
 * and CRUD operations.
 */
public class ProductManagementPanel extends JPanel {

    private final ProductController productController;
    private final CategoryController categoryController;
    private final List<Product> currentProducts = new ArrayList<>();
    private final List<Category> availableCategories = new ArrayList<>();

    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<CategoryFilterItem> categoryFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JLabel statusLabel;

    public ProductManagementPanel() {
        this(new ProductController(), new CategoryController());
    }

    public ProductManagementPanel(ProductController productController, CategoryController categoryController) {
        this.productController = productController;
        this.categoryController = categoryController;

        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        loadCategories();
        loadProducts();
    }

    private void initComponents() {
        // --- 1. Top Header & Action Bar ---
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));

        // Title and description
        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Product Catalog Management");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitleLabel = new JLabel("Manage master inventory items, SKUs, barcodes, and minimum stock alerts.");
        subtitleLabel.setForeground(Color.GRAY);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(subtitleLabel);
        topPanel.add(headerTextPanel, BorderLayout.WEST);

        // Right side: Category Filter, Status Filter, Search, Refresh, Add
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));
        categoryFilterCombo.addActionListener(e -> onFilterOrSearch());

        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "Active Only", "Inactive Only"});
        statusFilterCombo.addActionListener(e -> onFilterOrSearch());

        searchField = new JTextField(15);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, SKU, barcode...");
        searchField.addActionListener(e -> onFilterOrSearch());

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> onFilterOrSearch());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            categoryFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            loadCategories();
            loadProducts();
        });

        boolean canManage = UserSession.getInstance().isManagerOrAdmin();

        JButton addButton = new JButton("+ Add Product");
        addButton.putClientProperty("JButton.buttonType", "roundRect");
        addButton.setBackground(new Color(37, 99, 235));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(addButton.getFont().deriveFont(Font.BOLD));
        addButton.addActionListener(e -> onAddProduct());
        addButton.setVisible(canManage);

        actionPanel.add(new JLabel("Category:"));
        actionPanel.add(categoryFilterCombo);
        actionPanel.add(new JLabel("Status:"));
        actionPanel.add(statusFilterCombo);
        actionPanel.add(searchField);
        actionPanel.add(searchButton);
        actionPanel.add(refreshButton);
        if (canManage) {
            actionPanel.add(addButton);
        }
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- 2. Table in Center ---
        String[] columnNames = {"ID", "SKU", "Barcode", "Product Name", "Category", "Unit", "Min Stock", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        productTable = new JTable(tableModel);
        productTable.setRowHeight(32);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setShowGrid(true);
        productTable.setGridColor(new Color(241, 245, 249));

        // Column widths
        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        productTable.getColumnModel().getColumn(1).setPreferredWidth(110); // SKU
        productTable.getColumnModel().getColumn(2).setPreferredWidth(110); // Barcode
        productTable.getColumnModel().getColumn(3).setPreferredWidth(230); // Product Name
        productTable.getColumnModel().getColumn(4).setPreferredWidth(130); // Category
        productTable.getColumnModel().getColumn(5).setPreferredWidth(70);  // Unit
        productTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Min Stock
        productTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Status

        // Status Badge Renderer for column 7
        productTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());

        // Double-click row to edit (managers/admins only)
        productTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && productTable.getSelectedRow() >= 0) {
                    if (UserSession.getInstance().isManagerOrAdmin()) {
                        onEditProduct();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Bottom Bar ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        statusLabel = new JLabel("Loading product catalog...");
        statusLabel.setForeground(Color.GRAY);

        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton editButton = new JButton("Edit Product");
        editButton.addActionListener(e -> onEditProduct());

        JButton toggleButton = new JButton("Toggle Status");
        toggleButton.addActionListener(e -> onToggleStatus());

        JButton deleteButton = new JButton("Delete Product");
        deleteButton.setForeground(new Color(220, 38, 38));
        deleteButton.addActionListener(e -> onDeleteProduct());

        if (canManage) {
            bottomButtons.add(editButton);
            bottomButtons.add(toggleButton);
            bottomButtons.add(deleteButton);
        }

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(bottomButtons, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadCategories() {
        categoryController.loadCategories(
                categories -> {
                    availableCategories.clear();
                    availableCategories.addAll(categories);

                    CategoryFilterItem selected = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
                    Integer previousCatId = selected != null ? selected.getCategoryId() : null;

                    categoryFilterCombo.removeAllItems();
                    categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));

                    int toSelect = 0;
                    for (int i = 0; i < categories.size(); i++) {
                        Category cat = categories.get(i);
                        categoryFilterCombo.addItem(new CategoryFilterItem(cat.getCategoryId(), cat.getCategoryName()));
                        if (previousCatId != null && previousCatId.equals(cat.getCategoryId())) {
                            toSelect = i + 1;
                        }
                    }
                    categoryFilterCombo.setSelectedIndex(toSelect);
                },
                error -> statusLabel.setText("Could not load categories: " + error)
        );
    }

    private void loadProducts() {
        statusLabel.setText("Loading products...");
        CategoryFilterItem catItem = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
        Integer categoryId = catItem != null ? catItem.getCategoryId() : null;
        Boolean activeOnly = getActiveFilterSelection();

        productController.loadProducts(
                categoryId,
                activeOnly,
                this::populateTable,
                error -> {
                    statusLabel.setText("Error loading products: " + error);
                    JOptionPane.showMessageDialog(this, error, "Database Error", JOptionPane.ERROR_MESSAGE);
                }
        );
    }

    private void onFilterOrSearch() {
        String keyword = searchField.getText().trim();
        CategoryFilterItem catItem = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
        Integer categoryId = catItem != null ? catItem.getCategoryId() : null;
        Boolean activeOnly = getActiveFilterSelection();

        statusLabel.setText("Searching products...");
        productController.searchProducts(
                keyword,
                categoryId,
                activeOnly,
                this::populateTable,
                error -> {
                    statusLabel.setText("Search error: " + error);
                    JOptionPane.showMessageDialog(this, error, "Search Error", JOptionPane.ERROR_MESSAGE);
                }
        );
    }

    private Boolean getActiveFilterSelection() {
        int index = statusFilterCombo.getSelectedIndex();
        if (index == 1) return true;  // Active Only
        if (index == 2) return false; // Inactive Only
        return null;                  // All Statuses
    }

    private void populateTable(List<Product> products) {
        currentProducts.clear();
        currentProducts.addAll(products);

        tableModel.setRowCount(0);
        for (Product p : products) {
            tableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getSku(),
                    p.getBarcode() != null ? p.getBarcode() : "-",
                    p.getProductName(),
                    p.getCategoryName() != null ? p.getCategoryName() : "Category #" + p.getCategoryId(),
                    p.getUnit(),
                    p.getMinStockLevel(),
                    p.isActive()
            });
        }
        statusLabel.setText("Showing " + products.size() + " product(s).");
    }

    private void onAddProduct() {
        if (!UserSession.getInstance().isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Access denied: Administrator or Manager role required.", "Unauthorized", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ProductFormDialog dialog = new ProductFormDialog(parentWindow, productController, categoryController, null);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            loadProducts();
        }
    }

    private void onEditProduct() {
        if (!UserSession.getInstance().isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Access denied: Administrator or Manager role required.", "Unauthorized", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Product selectedProduct = currentProducts.get(selectedRow);
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ProductFormDialog dialog = new ProductFormDialog(parentWindow, productController, categoryController, selectedProduct);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            loadProducts();
        }
    }

    private void onToggleStatus() {
        if (!UserSession.getInstance().isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Access denied: Administrator or Manager role required.", "Unauthorized", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to toggle status.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Product selected = currentProducts.get(selectedRow);
        String action = selected.isActive() ? "deactivate" : "activate";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action + " '" + selected.getProductName() + "'?",
                "Confirm Status Change",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean updated = productController.handleToggleStatus(selected.getProductId(), error ->
                    JOptionPane.showMessageDialog(this, error, "Action Failed", JOptionPane.ERROR_MESSAGE)
            );
            if (updated) {
                loadProducts();
            }
        }
    }

    private void onDeleteProduct() {
        if (!UserSession.getInstance().isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Access denied: Administrator or Manager role required.", "Unauthorized", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Product selected = currentProducts.get(selectedRow);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete product '" + selected.getProductName() + "' (SKU: " + selected.getSku() + ")?\n" +
                        "This operation cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean deleted = productController.handleDeleteProduct(selected.getProductId(), error ->
                    JOptionPane.showMessageDialog(this, error, "Cannot Delete Product", JOptionPane.ERROR_MESSAGE)
            );
            if (deleted) {
                loadProducts();
            }
        }
    }

    /**
     * Helper item for Category Filter ComboBox.
     */
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
