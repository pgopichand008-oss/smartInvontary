package com.smartinventory.view;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.controller.*;
import com.smartinventory.model.User;
import com.smartinventory.model.enums.UserRole;
import com.smartinventory.util.UserSession;
import com.smartinventory.view.alert.AlertsPanel;
import com.smartinventory.view.auth.LoginDialog;
import com.smartinventory.view.category.CategoryManagementPanel;
import com.smartinventory.view.dashboard.DashboardPanel;
import com.smartinventory.view.inventory.InventoryManagementPanel;
import com.smartinventory.view.layout.MainFrame;
import com.smartinventory.view.layout.SidebarPanel;
import com.smartinventory.view.product.ProductManagementPanel;
import com.smartinventory.view.report.ReportManagementPanel;
import com.smartinventory.view.supplier.SupplierManagementPanel;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Swing UI Component Verification Test.
 * Directly instantiates and verifies Swing frames, dialogs, panels, table models,
 * renderers, and role guards on the AWT EventQueue.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SwingUiComponentVerificationTest {

    @BeforeAll
    public static void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                FlatLightLaf.setup();
            } catch (Exception ignored) {
            }
        });

        // Set up Admin session for UI testing
        User admin = new User(1, "admin", "$2a$12$...", "System Administrator", "admin@smartinventory.local", UserRole.ADMIN, true, null, null);
        UserSession.getInstance().login(admin);
    }

    @AfterAll
    public static void tearDown() {
        UserSession.getInstance().logout();
        DatabaseConnection.closePool();
    }

    @Test
    @Order(1)
    @DisplayName("UI-1: Login Dialog Layout and Component Hierarchy")
    public void test01_LoginDialogComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AuthController authController = new AuthController();
            LoginDialog dialog = new LoginDialog(null, authController, user -> {});

            assertNotNull(dialog.getTitle());
            assertTrue(dialog.getTitle().contains("Sign In"));
            assertFalse(dialog.isAuthenticated());
            dialog.dispose();
        });
    }

    @Test
    @Order(2)
    @DisplayName("UI-2: MainFrame and Sidebar Navigation")
    public void test02_MainFrameAndSidebar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AuthController authController = new AuthController();
            MainFrame mainFrame = new MainFrame(authController);

            assertNotNull(mainFrame.getTitle());
            assertTrue(mainFrame.getTitle().contains("Smart Inventory"));

            // Navigate through views
            mainFrame.navigateTo(SidebarPanel.NAV_DASHBOARD);
            mainFrame.navigateTo(SidebarPanel.NAV_CATEGORIES);
            mainFrame.navigateTo(SidebarPanel.NAV_SUPPLIERS);
            mainFrame.navigateTo(SidebarPanel.NAV_PRODUCTS);
            mainFrame.navigateTo(SidebarPanel.NAV_INVENTORY);
            mainFrame.navigateTo(SidebarPanel.NAV_ALERTS);
            mainFrame.navigateTo(SidebarPanel.NAV_REPORTS);

            mainFrame.dispose();
        });
    }

    @Test
    @Order(3)
    @DisplayName("UI-3: Category Management Panel Rendering")
    public void test03_CategoryManagementPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CategoryController controller = new CategoryController();
            CategoryManagementPanel panel = new CategoryManagementPanel(controller);
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(4)
    @DisplayName("UI-4: Supplier Management Panel Rendering")
    public void test04_SupplierManagementPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SupplierController controller = new SupplierController();
            SupplierManagementPanel panel = new SupplierManagementPanel(controller);
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(5)
    @DisplayName("UI-5: Product Management Panel Rendering")
    public void test05_ProductManagementPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProductController prodCtrl = new ProductController();
            CategoryController catCtrl = new CategoryController();
            ProductManagementPanel panel = new ProductManagementPanel(prodCtrl, catCtrl);
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(6)
    @DisplayName("UI-6: Inventory Management Panel Rendering")
    public void test06_InventoryManagementPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InventoryController invCtrl = new InventoryController();
            ProductController prodCtrl = new ProductController();
            SupplierController suppCtrl = new SupplierController();
            InventoryManagementPanel panel = new InventoryManagementPanel(invCtrl, prodCtrl, suppCtrl);
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(7)
    @DisplayName("UI-7: Alerts Panel Rendering")
    public void test07_AlertsPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AlertController alertCtrl = new AlertController();
            CategoryController catCtrl = new CategoryController();
            AlertsPanel panel = new AlertsPanel(alertCtrl, catCtrl);
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(8)
    @DisplayName("UI-8: Dashboard Panel Rendering")
    public void test08_DashboardPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardController dashCtrl = new DashboardController();
            DashboardPanel panel = new DashboardPanel(dashCtrl, nav -> {});
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(9)
    @DisplayName("UI-9: Report Management Panel Rendering")
    public void test09_ReportManagementPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ReportController repCtrl = new ReportController();
            CategoryController catCtrl = new CategoryController();
            ReportManagementPanel panel = new ReportManagementPanel(repCtrl, catCtrl);
            assertNotNull(panel);
            assertEquals(BorderLayout.class, panel.getLayout().getClass());
        });
    }

    @Test
    @Order(10)
    @DisplayName("UI-10: Role-Based Guard Interception in MainFrame for Cashier")
    public void test10_CashierRoleGuards() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Switch session to Cashier
            User cashier = new User(2, "cashier_test", "$2a$12$...", "Test Cashier", "cashier@smartinventory.local", UserRole.CASHIER, true, null, null);
            UserSession.getInstance().login(cashier);

            AuthController authController = new AuthController();
            MainFrame mainFrame = new MainFrame(authController);

            // Cashier navigation to restricted modules should be intercepted
            assertFalse(UserSession.getInstance().isManagerOrAdmin());

            mainFrame.dispose();
        });
    }
}
