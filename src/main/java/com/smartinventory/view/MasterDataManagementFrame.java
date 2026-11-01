package com.smartinventory.view;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartinventory.view.category.CategoryManagementPanel;
import com.smartinventory.view.supplier.SupplierManagementPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Modern Swing Frame combining Category and Supplier Management views in a tabbed layout.
 */
public class MasterDataManagementFrame extends JFrame {

    public MasterDataManagementFrame() {
        super("Smart Inventory & Expiry Management - Master Data (Phase 2)");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1100, 680));
        setMinimumSize(new Dimension(850, 500));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 14f));

        CategoryManagementPanel categoryPanel = new CategoryManagementPanel();
        SupplierManagementPanel supplierPanel = new SupplierManagementPanel();

        tabbedPane.addTab("  Product Categories  ", categoryPanel);
        tabbedPane.addTab("  Suppliers & Vendors  ", supplierPanel);

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize modern FlatLaf Look and Feel
                FlatLightLaf.setup();
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
            } catch (Exception e) {
                System.err.println("Could not initialize FlatLaf Look and Feel: " + e.getMessage());
            }

            MasterDataManagementFrame frame = new MasterDataManagementFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
