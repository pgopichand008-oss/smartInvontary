package com.smartinventory.view.category;

import com.smartinventory.controller.CategoryController;
import com.smartinventory.model.Category;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Management Panel for Categories: lists, searches, creates, edits, and deletes categories.
 */
public class CategoryManagementPanel extends JPanel {

    private final CategoryController controller;
    private final List<Category> currentCategories = new ArrayList<>();

    private JTable categoryTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel statusLabel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CategoryManagementPanel() {
        this(new CategoryController());
    }

    public CategoryManagementPanel(CategoryController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        loadCategories();
    }

    private void initComponents() {
        // --- 1. Top Header & Action Bar ---
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));

        // Title and description
        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Category Management");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitleLabel = new JLabel("Classify products into departments and inventory groups.");
        subtitleLabel.setForeground(Color.GRAY);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(subtitleLabel);
        topPanel.add(headerTextPanel, BorderLayout.WEST);

        // Right side: Search field and Add button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        searchField = new JTextField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search categories...");
        searchField.addActionListener(e -> onSearch());

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> onSearch());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadCategories();
        });

        JButton addButton = new JButton("+ Add Category");
        addButton.putClientProperty("JButton.buttonType", "roundRect");
        addButton.setBackground(new Color(37, 99, 235));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> onAddCategory());

        actionPanel.add(searchField);
        actionPanel.add(searchButton);
        actionPanel.add(refreshButton);
        actionPanel.add(addButton);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- 2. Table in Center ---
        String[] columnNames = {"ID", "Category Name", "Description", "Created At"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        categoryTable = new JTable(tableModel);
        categoryTable.setRowHeight(32);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryTable.setShowGrid(true);
        categoryTable.setGridColor(new Color(241, 245, 249));

        // Column widths
        categoryTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        categoryTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        categoryTable.getColumnModel().getColumn(2).setPreferredWidth(350);
        categoryTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Double click to edit
        categoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && categoryTable.getSelectedRow() != -1) {
                    onEditCategory();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(categoryTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Bottom Toolbar ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Loading categories...");
        statusLabel.setForeground(Color.DARK_GRAY);
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel rowActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> onEditCategory());

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setForeground(new Color(220, 38, 38));
        deleteButton.addActionListener(e -> onDeleteCategory());

        rowActionPanel.add(editButton);
        rowActionPanel.add(deleteButton);
        bottomPanel.add(rowActionPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void loadCategories() {
        statusLabel.setText("Loading categories from database...");
        controller.loadCategories(
                list -> {
                    currentCategories.clear();
                    currentCategories.addAll(list);
                    renderTable();
                    statusLabel.setText("Total Categories: " + list.size());
                },
                error -> {
                    statusLabel.setText("Error loading categories.");
                    JOptionPane.showMessageDialog(this, error, "Database Error", JOptionPane.ERROR_MESSAGE);
                }
        );
    }

    private void onSearch() {
        String keyword = searchField.getText().trim();
        statusLabel.setText("Searching...");
        controller.searchCategories(
                keyword,
                list -> {
                    currentCategories.clear();
                    currentCategories.addAll(list);
                    renderTable();
                    statusLabel.setText("Found " + list.size() + " category(ies) matching '" + keyword + "'");
                },
                error -> JOptionPane.showMessageDialog(this, error, "Search Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        for (Category c : currentCategories) {
            tableModel.addRow(new Object[]{
                    c.getCategoryId(),
                    c.getCategoryName(),
                    c.getDescription() != null ? c.getDescription() : "",
                    c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
            });
        }
    }

    private void onAddCategory() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        CategoryFormDialog dialog = new CategoryFormDialog(parent, controller, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadCategories();
        }
    }

    private void onEditCategory() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a category to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Category category = currentCategories.get(selectedRow);
        Window parent = SwingUtilities.getWindowAncestor(this);
        CategoryFormDialog dialog = new CategoryFormDialog(parent, controller, category);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadCategories();
        }
    }

    private void onDeleteCategory() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a category to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Category category = currentCategories.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete category '" + category.getCategoryName() + "'?\n" +
                        "This operation cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean deleted = controller.handleDeleteCategory(category.getCategoryId(), errorMsg -> {
                JOptionPane.showMessageDialog(this, errorMsg, "Cannot Delete Category", JOptionPane.ERROR_MESSAGE);
            });
            if (deleted) {
                loadCategories();
            }
        }
    }
}
