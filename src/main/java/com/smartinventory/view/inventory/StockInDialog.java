package com.smartinventory.view.inventory;

import com.smartinventory.controller.InventoryController;
import com.smartinventory.controller.ProductController;
import com.smartinventory.controller.SupplierController;
import com.smartinventory.model.Batch;
import com.smartinventory.model.Product;
import com.smartinventory.model.Supplier;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modal dialog for Stock-In operations: receiving physical inventory and recording batch lots.
 */
public class StockInDialog extends JDialog {

    private final InventoryController inventoryController;
    private final ProductController productController;
    private final SupplierController supplierController;
    private boolean saved = false;

    private JComboBox<Product> productComboBox;
    private JComboBox<Supplier> supplierComboBox;
    private JTextField batchNumberField;
    private JSpinner quantitySpinner;
    private JSpinner purchasePriceSpinner;
    private JSpinner sellingPriceSpinner;
    private JTextField mfgDateField;
    private JTextField expDateField;
    private JTextField noteField;
    private JLabel errorLabel;

    public StockInDialog(Window owner, InventoryController inventoryController,
                         ProductController productController, SupplierController supplierController) {
        super(owner, "Receive Inventory - Stock In", ModalityType.APPLICATION_MODAL);
        this.inventoryController = inventoryController;
        this.productController = productController;
        this.supplierController = supplierController;

        initComponents();
        loadDropdownData();

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header Title
        JLabel titleLabel = new JLabel("Stock In: Receive New Batch Lot");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 1. Product
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Product: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        productComboBox = new JComboBox<>();
        productComboBox.setPreferredSize(new Dimension(280, 30));
        formPanel.add(productComboBox, gbc);

        // 2. Supplier
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Supplier: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        supplierComboBox = new JComboBox<>();
        supplierComboBox.setPreferredSize(new Dimension(280, 30));
        formPanel.add(supplierComboBox, gbc);

        // 3. Batch Number
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Batch Number: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel batchPanel = new JPanel(new BorderLayout(6, 0));
        batchNumberField = new JTextField(16);
        batchNumberField.putClientProperty("JTextField.placeholderText", "e.g., BAT-2026-001");
        JButton autoBatchBtn = new JButton("Generate");
        autoBatchBtn.addActionListener(e -> generateBatchNumber());
        batchPanel.add(batchNumberField, BorderLayout.CENTER);
        batchPanel.add(autoBatchBtn, BorderLayout.EAST);
        formPanel.add(batchPanel, gbc);

        // 4. Quantity
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Received Quantity: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(50, 1, 999999, 1));
        formPanel.add(quantitySpinner, gbc);

        // 5. Pricing (Purchase & Selling Price side-by-side)
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Pricing ($): *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel pricePanel = new JPanel(new GridLayout(1, 4, 8, 0));
        purchasePriceSpinner = new JSpinner(new SpinnerNumberModel(10.00, 0.00, 999999.00, 0.50));
        sellingPriceSpinner = new JSpinner(new SpinnerNumberModel(15.00, 0.00, 999999.00, 0.50));
        pricePanel.add(new JLabel("Cost:"));
        pricePanel.add(purchasePriceSpinner);
        pricePanel.add(new JLabel("Sell:", SwingConstants.RIGHT));
        pricePanel.add(sellingPriceSpinner);
        formPanel.add(pricePanel, gbc);

        // 6. Dates (Manufacturing Date & Expiry Date)
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Dates (YYYY-MM-DD): *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel datePanel = new JPanel(new GridLayout(1, 4, 8, 0));
        mfgDateField = new JTextField(LocalDate.now().minusDays(10).toString());
        expDateField = new JTextField(LocalDate.now().plusMonths(6).toString());
        datePanel.add(new JLabel("Mfg:"));
        datePanel.add(mfgDateField);
        datePanel.add(new JLabel("Exp: *", SwingConstants.RIGHT));
        datePanel.add(expDateField);
        formPanel.add(datePanel, gbc);

        // 7. Reference Note
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Reference Note:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        noteField = new JTextField(24);
        noteField.putClientProperty("JTextField.placeholderText", "e.g., PO #4401 / Delivery Slip #992");
        formPanel.add(noteField, gbc);

        // 8. Error message label
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

        JButton submitBtn = new JButton("Receive Stock In");
        submitBtn.putClientProperty("JButton.buttonType", "roundRect");
        submitBtn.setBackground(new Color(37, 99, 235));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD));
        submitBtn.addActionListener(e -> onSubmit());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(submitBtn);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    private void loadDropdownData() {
        productController.loadProducts(null, true,
                products -> {
                    productComboBox.removeAllItems();
                    for (Product p : products) {
                        productComboBox.addItem(p);
                    }
                },
                err -> errorLabel.setText("Failed to load products: " + err)
        );

        supplierController.loadSuppliers(true,
                suppliers -> {
                    supplierComboBox.removeAllItems();
                    for (Supplier s : suppliers) {
                        supplierComboBox.addItem(s);
                    }
                },
                err -> errorLabel.setText("Failed to load suppliers: " + err)
        );
    }

    private void generateBatchNumber() {
        Product p = (Product) productComboBox.getSelectedItem();
        String prefix = "BAT";
        if (p != null && p.getSku() != null) {
            String cleanSku = p.getSku().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (cleanSku.length() >= 3) {
                prefix = cleanSku.substring(0, Math.min(4, cleanSku.length()));
            }
        }
        long suffix = System.currentTimeMillis() % 1000000;
        batchNumberField.setText(prefix + "-" + suffix);
    }

    private void onSubmit() {
        errorLabel.setText(" ");

        Product selectedProduct = (Product) productComboBox.getSelectedItem();
        if (selectedProduct == null || selectedProduct.getProductId() == null) {
            errorLabel.setText("Please select a product.");
            return;
        }

        Supplier selectedSupplier = (Supplier) supplierComboBox.getSelectedItem();
        if (selectedSupplier == null || selectedSupplier.getSupplierId() == null) {
            errorLabel.setText("Please select a supplier.");
            return;
        }

        String batchNumber = batchNumberField.getText().trim();
        if (batchNumber.isEmpty()) {
            errorLabel.setText("Batch number is required.");
            batchNumberField.requestFocus();
            return;
        }

        int quantity = (Integer) quantitySpinner.getValue();
        BigDecimal purchasePrice = BigDecimal.valueOf(((Number) purchasePriceSpinner.getValue()).doubleValue());
        BigDecimal sellingPrice = BigDecimal.valueOf(((Number) sellingPriceSpinner.getValue()).doubleValue());

        if (sellingPrice.compareTo(purchasePrice) < 0) {
            errorLabel.setText("Selling price cannot be less than purchase price.");
            return;
        }

        LocalDate mfgDate = null;
        String mfgStr = mfgDateField.getText().trim();
        if (!mfgStr.isEmpty()) {
            try {
                mfgDate = LocalDate.parse(mfgStr);
            } catch (DateTimeParseException e) {
                errorLabel.setText("Invalid manufacturing date format (use YYYY-MM-DD).");
                return;
            }
        }

        LocalDate expDate;
        String expStr = expDateField.getText().trim();
        if (expStr.isEmpty()) {
            errorLabel.setText("Expiry date is required.");
            return;
        }
        try {
            expDate = LocalDate.parse(expStr);
        } catch (DateTimeParseException e) {
            errorLabel.setText("Invalid expiry date format (use YYYY-MM-DD).");
            return;
        }

        String note = noteField.getText().trim();

        Batch batch = new Batch(
                selectedProduct.getProductId(),
                selectedSupplier.getSupplierId(),
                batchNumber,
                purchasePrice,
                sellingPrice,
                quantity,
                quantity,
                mfgDate,
                expDate
        );

        inventoryController.handleStockIn(
                batch,
                null,
                note.isEmpty() ? null : note,
                created -> {
                    saved = true;
                    JOptionPane.showMessageDialog(this,
                            "Successfully received Batch " + created.getBatchNumber() + " with " +
                                    created.getInitialQuantity() + " units.",
                            "Stock In Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                },
                error -> errorLabel.setText(error)
        );
    }

    public boolean isSaved() {
        return saved;
    }
}
