package com.smartinventory.view.supplier;

import com.smartinventory.controller.SupplierController;
import com.smartinventory.model.Supplier;
import com.smartinventory.view.components.StatusBadgeRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Management Panel for Suppliers: lists, filters, searches, creates, edits, and manages vendor status.
 */
public class SupplierManagementPanel extends JPanel {

    private final SupplierController controller;
    private final List<Supplier> currentSuppliers = new ArrayList<>();

    private JTable supplierTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private JLabel statusLabel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public SupplierManagementPanel() {
        this(new SupplierController());
    }

    public SupplierManagementPanel(SupplierController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        loadSuppliers();
    }

    private void initComponents() {
        // --- 1. Top Header & Action Bar ---
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));

        // Title and description
        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Supplier & Vendor Directory");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitleLabel = new JLabel("Manage external vendors, contact points, and supply records.");
        subtitleLabel.setForeground(Color.GRAY);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(subtitleLabel);
        topPanel.add(headerTextPanel, BorderLayout.WEST);

        // Right side: Filter, Search, Refresh, Add
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        statusFilterCombo = new JComboBox<>(new String[]{"All Suppliers", "Active Only", "Inactive Only"});
        statusFilterCombo.addActionListener(e -> onFilterChange());

        searchField = new JTextField(16);
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, phone, email...");
        searchField.addActionListener(e -> onSearch());

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> onSearch());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            loadSuppliers();
        });

        JButton addButton = new JButton("+ Add Supplier");
        addButton.putClientProperty("JButton.buttonType", "roundRect");
        addButton.setBackground(new Color(37, 99, 235));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> onAddSupplier());

        actionPanel.add(new JLabel("Status:"));
        actionPanel.add(statusFilterCombo);
        actionPanel.add(searchField);
        actionPanel.add(searchButton);
        actionPanel.add(refreshButton);
        actionPanel.add(addButton);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- 2. Table in Center ---
        String[] columnNames = {"ID", "Company Name", "Contact Person", "Phone", "Email", "Address", "Status", "Created At"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        supplierTable = new JTable(tableModel);
        supplierTable.setRowHeight(32);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.setShowGrid(true);
        supplierTable.setGridColor(new Color(241, 245, 249));

        // Use custom StatusBadgeRenderer for the "Status" column (index 6)
        supplierTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        // Column widths
        supplierTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        supplierTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        supplierTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        supplierTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        supplierTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        supplierTable.getColumnModel().getColumn(5).setPreferredWidth(220);
        supplierTable.getColumnModel().getColumn(6).setPreferredWidth(90);
        supplierTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        // Double click to edit
        supplierTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && supplierTable.getSelectedRow() != -1) {
                    onEditSupplier();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(supplierTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Bottom Toolbar ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Loading suppliers...");
        statusLabel.setForeground(Color.DARK_GRAY);
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel rowActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton toggleStatusButton = new JButton("Toggle Active/Inactive");
        toggleStatusButton.addActionListener(e -> onToggleStatus());

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> onEditSupplier());

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setForeground(new Color(220, 38, 38));
        deleteButton.addActionListener(e -> onDeleteSupplier());

        rowActionPanel.add(toggleStatusButton);
        rowActionPanel.add(editButton);
        rowActionPanel.add(deleteButton);
        bottomPanel.add(rowActionPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void loadSuppliers() {
        statusLabel.setText("Loading suppliers from database...");
        Boolean activeFilter = getSelectedActiveFilter();

        controller.loadSuppliers(
                activeFilter,
                list -> {
                    currentSuppliers.clear();
                    currentSuppliers.addAll(list);
                    renderTable();
                    statusLabel.setText("Total Suppliers: " + list.size());
                },
                error -> {
                    statusLabel.setText("Error loading suppliers.");
                    JOptionPane.showMessageDialog(this, error, "Database Error", JOptionPane.ERROR_MESSAGE);
                }
        );
    }

    private Boolean getSelectedActiveFilter() {
        int idx = statusFilterCombo.getSelectedIndex();
        if (idx == 1) return true;
        if (idx == 2) return false;
        return null;
    }

    private void onFilterChange() {
        if (!searchField.getText().trim().isEmpty()) {
            onSearch();
        } else {
            loadSuppliers();
        }
    }

    private void onSearch() {
        String keyword = searchField.getText().trim();
        Boolean activeFilter = getSelectedActiveFilter();
        statusLabel.setText("Searching suppliers...");

        controller.searchSuppliers(
                keyword,
                activeFilter,
                list -> {
                    currentSuppliers.clear();
                    currentSuppliers.addAll(list);
                    renderTable();
                    statusLabel.setText("Found " + list.size() + " supplier(s) matching '" + keyword + "'");
                },
                error -> JOptionPane.showMessageDialog(this, error, "Search Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        for (Supplier s : currentSuppliers) {
            tableModel.addRow(new Object[]{
                    s.getSupplierId(),
                    s.getSupplierName(),
                    s.getContactPerson() != null ? s.getContactPerson() : "-",
                    s.getPhone(),
                    s.getEmail() != null ? s.getEmail() : "-",
                    s.getAddress() != null ? s.getAddress() : "-",
                    s.isActive() ? "Active" : "Inactive",
                    s.getCreatedAt() != null ? dateFormat.format(s.getCreatedAt()) : ""
            });
        }
    }

    private void onAddSupplier() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        SupplierFormDialog dialog = new SupplierFormDialog(parent, controller, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadSuppliers();
        }
    }

    private void onEditSupplier() {
        int selectedRow = supplierTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Supplier supplier = currentSuppliers.get(selectedRow);
        Window parent = SwingUtilities.getWindowAncestor(this);
        SupplierFormDialog dialog = new SupplierFormDialog(parent, controller, supplier);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadSuppliers();
        }
    }

    private void onToggleStatus() {
        int selectedRow = supplierTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Supplier supplier = currentSuppliers.get(selectedRow);
        boolean newStatus = !supplier.isActive();
        String actionName = newStatus ? "activate" : "deactivate";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Do you want to " + actionName + " supplier '" + supplier.getSupplierName() + "'?",
                "Confirm Status Change",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean updated = controller.handleToggleActiveStatus(supplier.getSupplierId(), newStatus, errorMsg -> {
                JOptionPane.showMessageDialog(this, errorMsg, "Update Error", JOptionPane.ERROR_MESSAGE);
            });
            if (updated) {
                loadSuppliers();
            }
        }
    }

    private void onDeleteSupplier() {
        int selectedRow = supplierTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Supplier supplier = currentSuppliers.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete supplier '" + supplier.getSupplierName() + "'?\n" +
                        "Note: If this supplier has past product batches, deletion will be blocked and you can deactivate them instead.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean deleted = controller.handleDeleteSupplier(supplier.getSupplierId(), errorMsg -> {
                JOptionPane.showMessageDialog(this, errorMsg, "Cannot Delete Supplier", JOptionPane.ERROR_MESSAGE);
            });
            if (deleted) {
                loadSuppliers();
            }
        }
    }
}
