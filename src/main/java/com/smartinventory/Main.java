package com.smartinventory;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.controller.AuthController;
import com.smartinventory.util.AppIconUtil;
import com.smartinventory.view.auth.LoginDialog;
import com.smartinventory.view.layout.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Application Entry Point: Smart Inventory & Expiry Management System.
 * Bootstraps FlatLaf theme, verifies database connectivity, and launches authentication.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Initializing Smart Inventory & Expiry Management System...");

        // 1. Verify Database Connectivity
        boolean connected = DatabaseConnection.testConnection();
        if (!connected) {
            logger.error("Failed to connect to MySQL database. Please verify src/main/resources/db.properties.");
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to connect to MySQL database on localhost:3306.\n" +
                            "Please ensure MySQL Server is running and check 'src/main/resources/db.properties'.",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        logger.info("Database connection established successfully.");

        // 2. Launch UI on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize modern FlatLaf Look and Feel
                FlatLightLaf.setup();
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("TabbedPane.tabHeight", 36);

                // Configure operating system taskbar branding where supported
                AppIconUtil.applyToTaskbar();
            } catch (Exception e) {
                logger.warn("Could not initialize FlatLaf Look and Feel: {}", e.getMessage());
            }

            // Launch Authentication
            AuthController authController = new AuthController();
            LoginDialog loginDialog = new LoginDialog(null, authController, user -> {
                logger.info("Authentication complete. Launching MainFrame for user: {}", user.getUsername());
                MainFrame mainFrame = new MainFrame(authController);
                mainFrame.refreshUserSession();
                mainFrame.setVisible(true);
            });
            loginDialog.setVisible(true);
        });
    }
}
