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
import java.io.File;

/**
 * Application Entry Point: Smart Inventory & Expiry Management System.
 * Bootstraps FlatLaf theme, verifies database connectivity, and launches authentication.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Initializing Smart Inventory & Expiry Management System...");

        // 1. Initialize modern FlatLaf Look and Feel first so error dialogs are properly styled
        try {
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

        // 2. Verify Database Connectivity with clear diagnostics
        boolean connected = DatabaseConnection.testConnection();
        if (!connected) {
            String sourceDesc = DatabaseConnection.getConfigSourceDescription();
            String errorMsg = DatabaseConnection.getLastConnectionErrorMessage();
            File appDir = DatabaseConnection.getApplicationDirectory();
            File displayDir = appDir.getName().equalsIgnoreCase("app") && appDir.getParentFile() != null
                    ? appDir.getParentFile()
                    : appDir;

            logger.error("Failed to connect to MySQL database. Source: {}, Error: {}", sourceDesc, errorMsg);

            String dialogMessage = String.format(
                    "<html><body style='width: 440px; font-family: sans-serif;'>" +
                    "<h3 style='color: #dc2626; margin-top: 0;'>Database Connection Failed</h3>" +
                    "<p>The application could not establish a connection to the MySQL database.</p>" +
                    "<p><b>Configuration Source:</b> %s</p>" +
                    "<p><b>Error Details:</b> <span style='color: #b91c1c;'>%s</span></p>" +
                    "<hr style='border: 0; border-top: 1px solid #cbd5e1; margin: 10px 0;'/>" +
                    "<p><b>Troubleshooting Steps:</b></p>" +
                    "<ol style='margin-left: 20px; padding-left: 0;'>" +
                    "<li>Ensure MySQL Server (8.0+) is installed and running on <code>localhost:3306</code>.</li>" +
                    "<li>Verify that <code>db.properties</code> exists in: <br/><code>%s</code></li>" +
                    "<li>Check that <code>db.user</code> and <code>db.password</code> match your MySQL credentials.</li>" +
                    "<li>If you are setting up for the first time, copy <code>db.properties.example</code> to <code>db.properties</code> and import the schema using <code>mysql -u root -p smart_inventory &lt; schema.sql</code>.</li>" +
                    "</ol>" +
                    "</body></html>",
                    escapeHtml(sourceDesc),
                    escapeHtml(errorMsg != null ? errorMsg : "Connection refused or unreachable"),
                    escapeHtml(displayDir.getAbsolutePath())
            );

            JOptionPane.showMessageDialog(
                    null,
                    dialogMessage,
                    "Database Connection Error - Smart Inventory",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        logger.info("Database connection established successfully. Config source: {}",
                DatabaseConnection.getConfigSourceDescription());

        // 3. Launch UI on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
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

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
