package com.smartinventory.view.product;

import com.smartinventory.controller.CategoryController;
import com.smartinventory.controller.ProductController;
import com.smartinventory.model.Category;
import com.smartinventory.model.Product;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Modern modal dialog for Creating and Editing Products in the Catalog.
 */
public class ProductFormDialog extends JDialog {

    private final ProductController productController;
    private final CategoryController categoryController;
    private final Product existingProduct;
    private boolean saved = false;

    private JComboBox<Category> categoryComboBox;
    private JTextField nameField;
    private JTextField skuField;
    private JTextField barcodeField;
    private JComboBox<String> unitComboBox;
    private JSpinner minStockSpinner;
    private JCheckBox activeCheckBox;
    private JTextArea descArea;
    private JLabel errorLabel;

    public ProductFormDialog(Window owner, ProductController productController, 
                             CategoryController categoryController, Product existingProduct) {
        super(owner, existingProduct == null ? "Add Product" : "Edit Product", ModalityType.APPLICATION_MODAL);
        this.productController = productController;
        this.categoryController = categoryController;
        this.existingProduct = existingProduct;

        initComponents();
        loadCategories();

        if (existingProduct != null) {
            populateFields();
        }

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header Title
        JLabel titleLabel = new JLabel(existingProduct == null ? "Create New Product" : "Edit Product Details");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 1. Category
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Category: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        categoryComboBox = new JComboBox<>();
        categoryComboBox.setPreferredSize(new Dimension(280, 30));
        formPanel.add(categoryComboBox, gbc);

        // 2. Product Name
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Product Name: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField(24);
        nameField.putClientProperty("JTextField.placeholderText", "e.g., Coca Cola 500ml, Organic Milk 1L");
        formPanel.add(nameField, gbc);

        // 3. SKU
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("SKU Code: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel skuPanel = new JPanel(new BorderLayout(6, 0));
        skuField = new JTextField(16);
        skuField.putClientProperty("JTextField.placeholderText", "e.g., BEV-COCA-500");
        JButton autoSkuButton = new JButton("Generate");
        autoSkuButton.setToolTipText("Generate a unique SKU code based on timestamp");
        autoSkuButton.addActionListener(e -> generateSkuSuggestion());
        skuPanel.add(skuField, BorderLayout.CENTER);
        skuPanel.add(autoSkuButton, BorderLayout.EAST);
        formPanel.add(skuPanel, gbc);

        // 4. Barcode
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Barcode / UPC:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        barcodeField = new JTextField(24);
        barcodeField.putClientProperty("JTextField.placeholderText", "Scan or enter barcode (optional)");
        formPanel.add(barcodeField, gbc);

        // 5. Unit & Min Stock Level (Side-by-side)
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Unit of Measure:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel unitStockPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        String[] units = {"pcs", "box", "pack", "bottle", "can", "kg", "g", "liter", "meter"};
        unitComboBox = new JComboBox<>(units);
        unitComboBox.setEditable(true);

        minStockSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 999999, 1));

        unitStockPanel.add(unitComboBox);
        unitStockPanel.add(new JLabel("Min Stock Alert: *", SwingConstants.RIGHT));
        unitStockPanel.add(minStockSpinner);
        formPanel.add(unitStockPanel, gbc);

        // 6. Active Status Checkbox
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Status:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        activeCheckBox = new JCheckBox("Active in product catalog", true);
        formPanel.add(activeCheckBox, gbc);

        // 7. Description
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descArea = new JTextArea(3, 24);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(descArea);
        formPanel.add(scrollPane, gbc);

        // 8. Error message label
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        formPanel.add(errorLabel, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = new JButton(existingProduct == null ? "Create Product" : "Save Changes");
        saveButton.putClientProperty("JButton.buttonType", "roundRect");
        saveButton.setBackground(new Color(37, 99, 235));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.addActionListener(e -> onSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    private void loadCategories() {
        categoryController.loadCategories(
                categories -> {
                    categoryComboBox.removeAllItems();
                    for (Category cat : categories) {
                        categoryComboBox.addItem(cat);
                    }
                    if (existingProduct != null) {
                        selectCategoryById(existingProduct.getCategoryId());
                    }
                },
                error -> errorLabel.setText("Could not load categories: " + error)
        );
    }

    private void selectCategoryById(int categoryId) {
        for (int i = 0; i < categoryComboBox.getItemCount(); i++) {
            Category cat = categoryComboBox.getItemAt(i);
            if (cat != null && cat.getCategoryId() != null && cat.getCategoryId() == categoryId) {
                categoryComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void populateFields() {
        nameField.setText(existingProduct.getProductName());
        skuField.setText(existingProduct.getSku());
        barcodeField.setText(existingProduct.getBarcode() != null ? existingProduct.getBarcode() : "");
        if (existingProduct.getUnit() != null) {
            unitComboBox.setSelectedItem(existingProduct.getUnit());
        }
        minStockSpinner.setValue(Math.max(1, existingProduct.getMinStockLevel()));
        activeCheckBox.setSelected(existingProduct.isActive());
        descArea.setText(existingProduct.getDescription() != null ? existingProduct.getDescription() : "");
    }

    private void generateSkuSuggestion() {
        Category selectedCategory = (Category) categoryComboBox.getSelectedItem();
        String prefix = "PRD";
        if (selectedCategory != null && selectedCategory.getCategoryName() != null) {
            String clean = selectedCategory.getCategoryName().replaceAll("[^a-zA-Z]", "").toUpperCase();
            if (clean.length() >= 3) {
                prefix = clean.substring(0, 3);
            }
        }
        long suffix = System.currentTimeMillis() % 1000000;
        skuField.setText(prefix + "-" + suffix);
    }

    private void onSave() {
        errorLabel.setText(" ");

        Category selectedCategory = (Category) categoryComboBox.getSelectedItem();
        if (selectedCategory == null || selectedCategory.getCategoryId() == null) {
            errorLabel.setText("Please select a category.");
            return;
        }

        String name = nameField.getText().trim();
        String sku = skuField.getText().trim();
        String barcode = barcodeField.getText().trim();
        String unit = unitComboBox.getSelectedItem() != null ? unitComboBox.getSelectedItem().toString().trim() : "pcs";
        int minStock = (Integer) minStockSpinner.getValue();
        boolean active = activeCheckBox.isSelected();
        String desc = descArea.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Product Name is required.");
            nameField.requestFocus();
            return;
        }

        if (sku.isEmpty()) {
            errorLabel.setText("SKU Code is required.");
            skuField.requestFocus();
            return;
        }

        if (minStock <= 0) {
            errorLabel.setText("Minimum stock level must be greater than zero.");
            return;
        }

        boolean success;
        if (existingProduct == null) {
            Product newProduct = new Product(
                    selectedCategory.getCategoryId(),
                    sku,
                    barcode.isEmpty() ? null : barcode,
                    name,
                    desc.isEmpty() ? null : desc,
                    unit,
                    minStock,
                    active
            );
            success = productController.handleCreateProduct(newProduct, error -> errorLabel.setText(error));
        } else {
            existingProduct.setCategoryId(selectedCategory.getCategoryId());
            existingProduct.setCategoryName(selectedCategory.getCategoryName());
            existingProduct.setProductName(name);
            existingProduct.setSku(sku);
            existingProduct.setBarcode(barcode.isEmpty() ? null : barcode);
            existingProduct.setUnit(unit);
            existingProduct.setMinStockLevel(minStock);
            existingProduct.setActive(active);
            existingProduct.setDescription(desc.isEmpty() ? null : desc);

            success = productController.handleUpdateProduct(existingProduct, error -> errorLabel.setText(error));
        }

        if (success) {
            saved = true;
            dispose();
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
