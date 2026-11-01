package com.smartinventory.view.dashboard;

import com.smartinventory.controller.DashboardController;
import com.smartinventory.model.dashboard.DashboardMetrics;
import com.smartinventory.util.UserSession;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Modern Executive Dashboard featuring Real-time KPIs, Inventory Valuation,
 * Graphics2D Category Distribution Donut Chart, and Monthly Movement Bar Chart.
 */
public class DashboardPanel extends JPanel {

    private final DashboardController dashboardController;
    private final Consumer<String> navigationHandler;

    // KPI Metric Labels
    private JLabel totalProductsLabel;
    private JLabel totalStockLabel;
    private JLabel valuationCostLabel;
    private JLabel valuationRetailLabel;
    private JLabel categoriesSuppliersLabel;
    private JLabel expiryRiskLabel;
    private JLabel stockShortageLabel;

    // Chart Components
    private CategoryDistributionChartPanel categoryChart;
    private MonthlyMovementChartPanel movementChart;

    // Status & Last Updated
    private JLabel lastUpdatedLabel;
    private JButton refreshButton;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public DashboardPanel() {
        this(new DashboardController(), null);
    }

    public DashboardPanel(DashboardController dashboardController, Consumer<String> navigationHandler) {
        this.dashboardController = dashboardController;
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // --- 1. Top Section: Header & KPI Cards ---
        JPanel topPanel = new JPanel(new BorderLayout(12, 14));

        // Header Title & Greeting
        JPanel headerPanel = new JPanel(new BorderLayout(12, 4));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Dashboard & Analytics");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleBlock.add(titleLabel);

        String username = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getUsername() : "Operator";
        String role = UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getRole().getDisplayName() : "Staff";
        JLabel subtitleLabel = new JLabel("Welcome back, " + username + " (" + role + ") | Live inventory KPIs and automated surveillance.");
        subtitleLabel.setForeground(new Color(100, 116, 139));
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 13f));
        titleBlock.add(subtitleLabel);

        headerPanel.add(titleBlock, BorderLayout.WEST);

        // Header Action Controls (Refresh Button & Timestamp)
        JPanel actionBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        lastUpdatedLabel = new JLabel("Syncing...");
        lastUpdatedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lastUpdatedLabel.setForeground(new Color(148, 163, 184));
        actionBlock.add(lastUpdatedLabel);

        refreshButton = new JButton("Refresh Dashboard");
        refreshButton.putClientProperty("JButton.buttonType", "roundRect");
        refreshButton.setBackground(new Color(37, 99, 235));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshData());
        actionBlock.add(refreshButton);

        headerPanel.add(actionBlock, BorderLayout.EAST);
        topPanel.add(headerPanel, BorderLayout.NORTH);

        // --- KPI Metric Cards Grid (2 rows x 3 columns or 1 row x 6 columns) ---
        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, 14, 12));
        kpiGrid.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));

        // 1. Total Catalog Products
        totalProductsLabel = new JLabel("0");
        kpiGrid.add(createKpiCard(
                "Total Products",
                "Active catalog SKUs",
                new Color(239, 246, 255),
                new Color(29, 78, 216),
                totalProductsLabel,
                null
        ));

        // 2. Physical Stock on Hand
        totalStockLabel = new JLabel("0");
        kpiGrid.add(createKpiCard(
                "Stock on Hand",
                "Total available physical units",
                new Color(236, 253, 245),
                new Color(4, 120, 87),
                totalStockLabel,
                null
        ));

        // 3. Inventory Valuation
        valuationCostLabel = new JLabel("$0.00");
        valuationRetailLabel = new JLabel("Retail Potential: $0.00");
        kpiGrid.add(createKpiCard(
                "Inventory Valuation",
                "Cost valuation basis",
                new Color(238, 242, 255),
                new Color(67, 56, 202),
                valuationCostLabel,
                valuationRetailLabel
        ));

        // 4. Categories & Suppliers
        categoriesSuppliersLabel = new JLabel("0 Categories / 0 Suppliers");
        kpiGrid.add(createKpiCard(
                "Master Taxonomy",
                "Organized product categories & partners",
                new Color(250, 245, 255),
                new Color(126, 34, 206),
                categoriesSuppliersLabel,
                null
        ));

        // 5. Expiry Risk Surveillance
        expiryRiskLabel = new JLabel("0 Expired / 0 Soon");
        kpiGrid.add(createKpiCard(
                "Expiry Surveillance",
                "Expired lots / Expiring within 30 days",
                new Color(254, 242, 242),
                new Color(185, 28, 28),
                expiryRiskLabel,
                null
        ));

        // 6. Stock Shortage Alerts
        stockShortageLabel = new JLabel("0 Deficit / 0 Empty");
        kpiGrid.add(createKpiCard(
                "Reorder Alerts",
                "Below safety stock / Out of stock",
                new Color(255, 251, 235),
                new Color(180, 83, 9),
                stockShortageLabel,
                null
        ));

        topPanel.add(kpiGrid, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // --- 2. Center Section: Analytics Charts ---
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));

        // Chart 1: Category Distribution
        JPanel categoryCard = createChartCard("Stock Distribution by Category", "Breakdown of inventory by product categories");
        categoryChart = new CategoryDistributionChartPanel();
        categoryCard.add(categoryChart, BorderLayout.CENTER);
        chartsPanel.add(categoryCard);

        // Chart 2: Monthly Movement Trends
        JPanel movementCard = createChartCard("Monthly Inventory Movement Trends", "Last 6 months inflow, outflow & discards");
        movementChart = new MonthlyMovementChartPanel();
        movementCard.add(movementChart, BorderLayout.CENTER);
        chartsPanel.add(movementCard);

        add(chartsPanel, BorderLayout.CENTER);

        // --- 3. Bottom Section: Quick Actions & Shortcut Banner ---
        JPanel bottomPanel = new JPanel(new BorderLayout(12, 8));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JLabel quickLabel = new JLabel("Quick Actions:");
        quickLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        quickLabel.setForeground(new Color(71, 85, 105));
        quickActions.add(quickLabel);

        boolean isManagerOrAdmin = UserSession.getInstance().isManagerOrAdmin();

        if (isManagerOrAdmin) {
            JButton stockInBtn = createQuickButton("Receive Stock-In", new Color(16, 185, 129), () -> navigate("inventory"));
            quickActions.add(stockInBtn);
            JButton stockOutBtn = createQuickButton("Dispense Stock-Out (FEFO)", new Color(37, 99, 235), () -> navigate("inventory"));
            quickActions.add(stockOutBtn);
        }
        JButton alertsBtn = createQuickButton("Open Alert Center", new Color(239, 68, 68), () -> navigate("alerts"));
        quickActions.add(alertsBtn);
        if (isManagerOrAdmin) {
            JButton reportsBtn = createQuickButton("Export Reports", new Color(100, 116, 139), () -> navigate("reports"));
            quickActions.add(reportsBtn);
        }

        bottomPanel.add(quickActions, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createKpiCard(String title, String subtitle, Color bgColor, Color accentColor,
                                 JLabel mainLabel, JLabel secondaryLabel) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JPanel textBlock = new JPanel(new GridLayout(secondaryLabel != null ? 3 : 2, 1, 0, 2));
        textBlock.setOpaque(false);

        JLabel tLabel = new JLabel(title);
        tLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tLabel.setForeground(accentColor);
        textBlock.add(tLabel);

        mainLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        mainLabel.setForeground(new Color(15, 23, 42));
        textBlock.add(mainLabel);

        if (secondaryLabel != null) {
            secondaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            secondaryLabel.setForeground(new Color(100, 116, 139));
            textBlock.add(secondaryLabel);
        } else {
            JLabel sLabel = new JLabel(subtitle);
            sLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            sLabel.setForeground(new Color(100, 116, 139));
            textBlock.add(sLabel);
        }

        card.add(textBlock, BorderLayout.CENTER);
        return card;
    }

    private JPanel createChartCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);

        JLabel tLabel = new JLabel(title);
        tLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tLabel.setForeground(new Color(15, 23, 42));
        header.add(tLabel);

        JLabel sLabel = new JLabel(subtitle);
        sLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sLabel.setForeground(new Color(100, 116, 139));
        header.add(sLabel);

        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JButton createQuickButton(String text, Color color, Runnable action) {
        JButton btn = new JButton(text);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void navigate(String target) {
        if (navigationHandler != null) {
            navigationHandler.accept(target);
        }
    }

    /**
     * Public method to asynchronously refresh the dashboard with fresh database records.
     */
    public void refreshData() {
        refreshButton.setEnabled(false);
        lastUpdatedLabel.setText("Fetching metrics...");

        dashboardController.loadDashboardMetrics(
                metrics -> {
                    updateUIWithMetrics(metrics);
                    refreshButton.setEnabled(true);
                    lastUpdatedLabel.setText("Updated: " + timeFormat.format(new Date()));
                },
                errorMsg -> {
                    refreshButton.setEnabled(true);
                    lastUpdatedLabel.setText("Sync failed: " + errorMsg);
                    JOptionPane.showMessageDialog(this,
                            "Failed to load dashboard metrics:\n" + errorMsg,
                            "Dashboard Error", JOptionPane.ERROR_MESSAGE);
                }
        );
    }

    private void updateUIWithMetrics(DashboardMetrics m) {
        totalProductsLabel.setText(String.format("%,d SKUs", m.getTotalProducts()));
        totalStockLabel.setText(String.format("%,d Units", m.getTotalStock()));

        // Role-sensitive financial valuation: Only Managers and Admins can inspect complete valuation totals
        if (UserSession.getInstance().isManagerOrAdmin()) {
            valuationCostLabel.setText(currencyFormat.format(m.getTotalCostValuation()));
            valuationRetailLabel.setText("Retail: " + currencyFormat.format(m.getTotalRetailPotential()));
        } else {
            valuationCostLabel.setText("Restricted");
            valuationRetailLabel.setText("Manager role required");
        }

        categoriesSuppliersLabel.setText(m.getTotalCategories() + " Categories / " + m.getTotalSuppliers() + " Suppliers");
        expiryRiskLabel.setText(m.getExpiredBatches() + " Expired / " + m.getExpiringSoonBatches() + " Soon");
        stockShortageLabel.setText(m.getLowStockProducts() + " Low / " + m.getOutOfStockProducts() + " Empty");

        // Update charts
        categoryChart.setData(m.getCategoryDistribution());
        movementChart.setData(m.getMonthlyMovements());
    }
}
