package com.smartinventory.view.category;

import com.smartinventory.controller.CategoryController;
import com.smartinventory.model.Category;

import javax.swing.*;
import java.awt.*;

/**
 * Modern modal dialog for Creating and Editing Categories.
 */
public class CategoryFormDialog extends JDialog {

    private final CategoryController controller;
    private final Category existingCategory;
    private boolean saved = false;

    private JTextField nameField;
    private JTextArea descArea;
    private JLabel errorLabel;

    public CategoryFormDialog(Window owner, CategoryController controller, Category existingCategory) {
        super(owner, existingCategory == null ? "Add Category" : "Edit Category", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.existingCategory = existingCategory;

        initComponents();
        if (existingCategory != null) {
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
        JLabel titleLabel = new JLabel(existingCategory == null ? "Create New Category" : "Edit Category Details");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Name Label & Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Category Name: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField(25);
        nameField.putClientProperty("JTextField.placeholderText", "e.g., Beverages, Dairy, Snacks");
        formPanel.add(nameField, gbc);

        // Description Label & Area
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descArea = new JTextArea(4, 25);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(descArea);
        formPanel.add(scrollPane, gbc);

        // Error message label
        gbc.gridx = 0;
        gbc.gridy = 2;
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

        JButton saveButton = new JButton(existingCategory == null ? "Create Category" : "Save Changes");
        saveButton.putClientProperty("JButton.buttonType", "roundRect");
        saveButton.setBackground(new Color(37, 99, 235));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> onSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
        getRootPane().setDefaultButton(saveButton);
    }

    private void populateFields() {
        nameField.setText(existingCategory.getCategoryName());
        descArea.setText(existingCategory.getDescription() != null ? existingCategory.getDescription() : "");
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String desc = descArea.getText().trim();
        errorLabel.setText(" ");

        boolean success;
        if (existingCategory == null) {
            success = controller.handleCreateCategory(name, desc, msg -> errorLabel.setText(msg));
        } else {
            success = controller.handleUpdateCategory(existingCategory.getCategoryId(), name, desc, msg -> errorLabel.setText(msg));
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
