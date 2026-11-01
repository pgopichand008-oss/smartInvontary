package com.smartinventory.view.inventory;

import com.smartinventory.controller.InventoryController;
import com.smartinventory.controller.ProductController;
import com.smartinventory.model.Product;
import com.smartinventory.model.StockOutResult;
import com.smartinventory.model.enums.TransactionType;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Modal dialog for Stock-Out operations with First Expiry, First Out (FEFO) allocation.
 */
public class StockOutDialog extends JDialog {

    private final InventoryController inventoryController;
    private final ProductController productController;
    private boolean completed = false;

    private JComboBox<Product> productComboBox;
    private JLabel availableStockLabel;
    private JSpinner quantitySpinner;
    private JComboBox<TransactionType> typeComboBox;
    private JTextField noteField;
    private JLabel errorLabel;
    private int currentAvailableStock = 0;

    public StockOutDialog(Window owner, InventoryController inventoryController, ProductController productController) {
        super(owner, "Stock Out - FEFO Inventory Dispatch", ModalityType.APPLICATION_MODAL);
        this.inventoryController = inventoryController;
        this.productController = productController;

        initComponents();
        loadProducts();

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header Title
        JLabel titleLabel = new JLabel("Stock Out (FEFO Dispatch)");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 1. Product
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Product: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        productComboBox = new JComboBox<>();
        productComboBox.setPreferredSize(new Dimension(300, 30));
        productComboBox.addActionListener(e -> updateProductStock());
        formPanel.add(productComboBox, gbc);

        // 2. Available Stock Indicator
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Eligible Stock:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        availableStockLabel = new JLabel("Checking available stock...");
        availableStockLabel.setFont(availableStockLabel.getFont().deriveFont(Font.BOLD, 13f));
        availableStockLabel.setForeground(new Color(37, 99, 235));
        formPanel.add(availableStockLabel, gbc);

        // 3. Outgoing Quantity
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Quantity to Deduct: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999999, 1));
        formPanel.add(quantitySpinner, gbc);

        // 4. Movement Type
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Transaction Type: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        typeComboBox = new JComboBox<>(new TransactionType[]{
                TransactionType.SALE_OUT,
                TransactionType.DAMAGE_OUT,
                TransactionType.EXPIRED_WRITE_OFF
        });
        formPanel.add(typeComboBox, gbc);

        // 5. Reference Note
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Reference / Reason:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        noteField = new JTextField(24);
        noteField.putClientProperty("JTextField.placeholderText", "e.g., Sales Order #1002, Counter Sale, Damaged during transit");
        formPanel.add(noteField, gbc);

        // 6. FEFO Notice Banner
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel fefoNotice = new JLabel("<html><body style='width: 320px; color: #475569; font-size: 11px;'>" +
                "⚡ <b>FEFO Enforced:</b> Eligible batches with the earliest expiry dates will be automatically deducted first." +
                "</body></html>");
        fefoNotice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        formPanel.add(fefoNotice, gbc);

        // 7. Error message label
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        formPanel.add(errorLabel, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton submitBtn = new JButton("Execute Stock Out");
        submitBtn.putClientProperty("JButton.buttonType", "roundRect");
        submitBtn.setBackground(new Color(225, 29, 72));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD));
        submitBtn.addActionListener(e -> onSubmit());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(submitBtn);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    private void loadProducts() {
        productController.loadProducts(null, true,
                products -> {
                    productComboBox.removeAllItems();
                    for (Product p : products) {
                        productComboBox.addItem(p);
                    }
                    updateProductStock();
                },
                err -> errorLabel.setText("Failed to load products: " + err)
        );
    }

    private void updateProductStock() {
        Product p = (Product) productComboBox.getSelectedItem();
        if (p == null || p.getProductId() == null) {
            availableStockLabel.setText("0 units");
            currentAvailableStock = 0;
            return;
        }

        availableStockLabel.setText("Querying stock...");
        inventoryController.loadProductTotalStock(
                p.getProductId(),
                stock -> {
                    currentAvailableStock = stock;
                    availableStockLabel.setText(stock + " " + p.getUnit() + " available in eligible batches");
                    if (stock == 0) {
                        availableStockLabel.setForeground(new Color(220, 38, 38));
                    } else {
                        availableStockLabel.setForeground(new Color(22, 101, 52));
                    }
                },
                err -> availableStockLabel.setText("Error loading stock: " + err)
        );
    }

    private void onSubmit() {
        errorLabel.setText(" ");

        Product p = (Product) productComboBox.getSelectedItem();
        if (p == null || p.getProductId() == null) {
            errorLabel.setText("Please select a product.");
            return;
        }

        int requestedQty = (Integer) quantitySpinner.getValue();
        if (requestedQty <= 0) {
            errorLabel.setText("Quantity must be greater than zero.");
            return;
        }

        if (requestedQty > currentAvailableStock) {
            errorLabel.setText("Cannot dispatch: requested quantity (" + requestedQty +
                    ") exceeds available stock (" + currentAvailableStock + ").");
            return;
        }

        TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
        String note = noteField.getText().trim();

        inventoryController.handleStockOut(
                p.getProductId(),
                requestedQty,
                type,
                note.isEmpty() ? null : note,
                null,
                result -> {
                    completed = true;
                    JOptionPane.showMessageDialog(this,
                            result.getSummary(),
                            "Stock Out (FEFO) Completed",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                },
                error -> errorLabel.setText(error)
        );
    }

    public boolean isCompleted() {
        return completed;
    }
}
