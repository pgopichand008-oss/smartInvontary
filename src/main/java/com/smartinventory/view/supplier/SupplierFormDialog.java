package com.smartinventory.view.supplier;

import com.smartinventory.controller.SupplierController;
import com.smartinventory.model.Supplier;

import javax.swing.*;
import java.awt.*;

/**
 * Modern modal dialog for Creating and Editing Suppliers.
 */
public class SupplierFormDialog extends JDialog {

    private final SupplierController controller;
    private final Supplier existingSupplier;
    private boolean saved = false;

    private JTextField nameField;
    private JTextField contactPersonField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextArea addressArea;
    private JCheckBox activeCheckBox;
    private JLabel errorLabel;

    public SupplierFormDialog(Window owner, SupplierController controller, Supplier existingSupplier) {
        super(owner, existingSupplier == null ? "Add Supplier" : "Edit Supplier", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.existingSupplier = existingSupplier;

        initComponents();
        if (existingSupplier != null) {
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
        JLabel titleLabel = new JLabel(existingSupplier == null ? "Register New Supplier" : "Edit Supplier Details");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 1. Company Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Company Name: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField(24);
        nameField.putClientProperty("JTextField.placeholderText", "e.g., Nestle Distribution Ltd");
        formPanel.add(nameField, gbc);

        // 2. Contact Person
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Contact Person:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        contactPersonField = new JTextField(24);
        contactPersonField.putClientProperty("JTextField.placeholderText", "e.g., John Smith");
        formPanel.add(contactPersonField, gbc);

        // 3. Phone Number
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Phone Number: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        phoneField = new JTextField(24);
        phoneField.putClientProperty("JTextField.placeholderText", "e.g., +1-555-0199 or 0771234567");
        formPanel.add(phoneField, gbc);

        // 4. Email
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Email Address:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField(24);
        emailField.putClientProperty("JTextField.placeholderText", "e.g., orders@supplier.com");
        formPanel.add(emailField, gbc);

        // 5. Address
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Physical Address:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        addressArea = new JTextArea(3, 24);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(addressArea);
        formPanel.add(scrollPane, gbc);

        // 6. Active Status (for edits)
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        activeCheckBox = new JCheckBox("Active Supplier (can deliver new shipments)", true);
        formPanel.add(activeCheckBox, gbc);

        // Error message label
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        formPanel.add(errorLabel, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = new JButton(existingSupplier == null ? "Register Supplier" : "Save Changes");
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
        nameField.setText(existingSupplier.getSupplierName());
        contactPersonField.setText(existingSupplier.getContactPerson() != null ? existingSupplier.getContactPerson() : "");
        phoneField.setText(existingSupplier.getPhone());
        emailField.setText(existingSupplier.getEmail() != null ? existingSupplier.getEmail() : "");
        addressArea.setText(existingSupplier.getAddress() != null ? existingSupplier.getAddress() : "");
        activeCheckBox.setSelected(existingSupplier.isActive());
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String contact = contactPersonField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressArea.getText().trim();
        boolean active = activeCheckBox.isSelected();
        errorLabel.setText(" ");

        boolean success;
        if (existingSupplier == null) {
            success = controller.handleCreateSupplier(name, contact, phone, email, address, msg -> errorLabel.setText(msg));
        } else {
            success = controller.handleUpdateSupplier(existingSupplier.getSupplierId(), name, contact, phone, email, address, active, msg -> errorLabel.setText(msg));
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
