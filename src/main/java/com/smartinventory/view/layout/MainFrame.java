package com.smartinventory.view.layout;

import com.smartinventory.controller.AlertController;
import com.smartinventory.controller.AuthController;
import com.smartinventory.controller.CategoryController;
import com.smartinventory.controller.DashboardController;
import com.smartinventory.controller.InventoryController;
import com.smartinventory.controller.ProductController;
import com.smartinventory.controller.ReportController;
import com.smartinventory.controller.SupplierController;
import com.smartinventory.model.User;
import com.smartinventory.util.AppIconUtil;
import com.smartinventory.util.UserSession;
import com.smartinventory.view.alert.AlertsPanel;
import com.smartinventory.view.auth.LoginDialog;
import com.smartinventory.view.category.CategoryManagementPanel;
import com.smartinventory.view.dashboard.DashboardPanel;
import com.smartinventory.view.inventory.InventoryManagementPanel;
import com.smartinventory.view.product.ProductManagementPanel;
import com.smartinventory.view.report.ReportManagementPanel;
import com.smartinventory.view.supplier.SupplierManagementPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main Application Shell featuring Sidebar Navigation and CardLayout Content Switching.
 */
public class MainFrame extends JFrame {

    private final AuthController authController;
    private SidebarPanel sidebarPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private AlertsPanel alertsPanel;
    private DashboardPanel dashboardPanel;
    private ReportManagementPanel reportPanel;

    public MainFrame() {
        this(new AuthController());
    }

    public MainFrame(AuthController authController) {
        super("Smart Inventory & Expiry Management System");
        this.authController = authController;

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 750));
        setMinimumSize(new Dimension(950, 600));

        // Apply official application window icon branding
        AppIconUtil.applyToWindow(this);

        setLayout(new BorderLayout());

        // 1. Sidebar Navigation (West)
        sidebarPanel = new SidebarPanel(this::navigateTo, this::performLogout);
        add(sidebarPanel, BorderLayout.WEST);

        // 2. CardLayout Content Area (Center)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Register Modules
        CategoryController categoryController = new CategoryController();
        SupplierController supplierController = new SupplierController();
        ProductController productController = new ProductController();
        InventoryController inventoryController = new InventoryController();
        AlertController alertController = new AlertController();
        DashboardController dashboardController = new DashboardController();
        ReportController reportController = new ReportController();

        CategoryManagementPanel categoryPanel = new CategoryManagementPanel(categoryController);
        SupplierManagementPanel supplierPanel = new SupplierManagementPanel(supplierController);
        ProductManagementPanel productPanel = new ProductManagementPanel(productController, categoryController);
        InventoryManagementPanel inventoryPanel = new InventoryManagementPanel(inventoryController, productController, supplierController);
        alertsPanel = new AlertsPanel(alertController, categoryController);
        dashboardPanel = new DashboardPanel(dashboardController, nav -> {
            if ("inventory".equalsIgnoreCase(nav)) {
                navigateTo(SidebarPanel.NAV_INVENTORY);
            } else if ("alerts".equalsIgnoreCase(nav)) {
                navigateTo(SidebarPanel.NAV_ALERTS);
            } else if ("reports".equalsIgnoreCase(nav)) {
                navigateTo(SidebarPanel.NAV_REPORTS);
            }
        });
        reportPanel = new ReportManagementPanel(reportController, categoryController);

        // Auto-refresh alerts and dashboard whenever stock-in or stock-out operations complete
        inventoryController.getInventoryService().addStockChangeListener(() ->
                SwingUtilities.invokeLater(() -> {
                    if (alertsPanel != null) {
                        alertsPanel.refreshAlerts();
                    }
                    if (dashboardPanel != null) {
                        dashboardPanel.refreshData();
                    }
                })
        );

        contentPanel.add(dashboardPanel, SidebarPanel.NAV_DASHBOARD);
        contentPanel.add(categoryPanel, SidebarPanel.NAV_CATEGORIES);
        contentPanel.add(supplierPanel, SidebarPanel.NAV_SUPPLIERS);
        contentPanel.add(productPanel, SidebarPanel.NAV_PRODUCTS);
        contentPanel.add(inventoryPanel, SidebarPanel.NAV_INVENTORY);
        contentPanel.add(alertsPanel, SidebarPanel.NAV_ALERTS);
        contentPanel.add(reportPanel, SidebarPanel.NAV_REPORTS);

        add(contentPanel, BorderLayout.CENTER);

        // Default landing view: Dashboard
        navigateTo(SidebarPanel.NAV_DASHBOARD);

        pack();
        setLocationRelativeTo(null);
    }

    public void navigateTo(String navKey) {
        if (!UserSession.getInstance().isManagerOrAdmin()) {
            if (SidebarPanel.NAV_CATEGORIES.equals(navKey) ||
                SidebarPanel.NAV_SUPPLIERS.equals(navKey) ||
                SidebarPanel.NAV_INVENTORY.equals(navKey) ||
                SidebarPanel.NAV_REPORTS.equals(navKey)) {
                JOptionPane.showMessageDialog(this,
                        "Access Denied: You do not have permission to access this module.",
                        "Authorization Required",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        cardLayout.show(contentPanel, navKey);
        if (sidebarPanel != null) {
            sidebarPanel.setActiveNavKey(navKey);
        }
        if (SidebarPanel.NAV_DASHBOARD.equals(navKey) && dashboardPanel != null) {
            dashboardPanel.refreshData();
        } else if (SidebarPanel.NAV_ALERTS.equals(navKey) && alertsPanel != null) {
            alertsPanel.refreshAlerts();
        } else if (SidebarPanel.NAV_REPORTS.equals(navKey) && reportPanel != null) {
            reportPanel.generateSelectedReport();
        }
    }

    public void refreshUserSession() {
        if (sidebarPanel != null) {
            sidebarPanel.updateUserDisplay();
        }
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to sign out?",
                "Sign Out Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            authController.handleLogout();
            dispose();

            // Reopen Login Dialog on Swing Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                LoginDialog loginDialog = new LoginDialog(null, authController, authenticatedUser -> {
                    MainFrame newMainFrame = new MainFrame(authController);
                    newMainFrame.refreshUserSession();
                    newMainFrame.setVisible(true);
                });
                loginDialog.setVisible(true);
            });
        }
    }
}
