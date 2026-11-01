package com.smartinventory.view.layout;

import com.smartinventory.model.User;
import com.smartinventory.model.enums.UserRole;
import com.smartinventory.util.AppIconUtil;
import com.smartinventory.util.UserSession;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern FlatLaf-styled sidebar navigation panel with role-aware menu visibility.
 */
public class SidebarPanel extends JPanel {

    public static final String NAV_DASHBOARD = "DASHBOARD";
    public static final String NAV_CATEGORIES = "CATEGORIES";
    public static final String NAV_SUPPLIERS = "SUPPLIERS";
    public static final String NAV_PRODUCTS = "PRODUCTS";
    public static final String NAV_INVENTORY = "INVENTORY";
    public static final String NAV_ALERTS = "ALERTS";
    public static final String NAV_REPORTS = "REPORTS";

    private final Consumer<String> onNavigate;
    private final Runnable onLogout;
    private final List<JButton> navButtons = new ArrayList<>();
    private String activeNavKey = NAV_DASHBOARD; // default active view

    private JLabel userNameLabel;
    private JLabel userRoleBadge;

    public SidebarPanel(Consumer<String> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        this.onLogout = onLogout;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(240, 0));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(226, 232, 240)));

        initComponents();
        updateUserDisplay();
    }

    private void initComponents() {
        // --- 1. Top Logo Header ---
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));

        ImageIcon brandIcon = AppIconUtil.getScaledIcon(28, 28);
        JLabel logoIcon = brandIcon != null ? new JLabel(brandIcon) : new JLabel("⚡");
        if (brandIcon == null) {
            logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        }

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Smart Inventory");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JLabel version = new JLabel("v1.0 Desktop");
        version.setFont(version.getFont().deriveFont(Font.PLAIN, 11f));
        version.setForeground(new Color(100, 116, 139));
        textPanel.add(title);
        textPanel.add(version);

        headerPanel.add(logoIcon, BorderLayout.WEST);
        headerPanel.add(textPanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. Menu Navigation List ---
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        User currentUser = UserSession.getInstance().getCurrentUser();
        boolean isManagerOrAdmin = currentUser == null || currentUser.isManagerOrAdmin();

        addNavButton(menuPanel, "📊  Dashboard", NAV_DASHBOARD, true);
        addNavButton(menuPanel, "📁  Categories", NAV_CATEGORIES, isManagerOrAdmin);
        addNavButton(menuPanel, "🏢  Suppliers", NAV_SUPPLIERS, isManagerOrAdmin);
        addNavButton(menuPanel, "🏷️  Products", NAV_PRODUCTS, true);
        addNavButton(menuPanel, "📦  Inventory Batches", NAV_INVENTORY, isManagerOrAdmin);
        addNavButton(menuPanel, "⚠️  Expiry Alerts", NAV_ALERTS, true);
        addNavButton(menuPanel, "📈  Reports", NAV_REPORTS, isManagerOrAdmin);

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Bottom User Profile & Logout ---
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel userInfoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        userInfoPanel.setOpaque(false);

        userNameLabel = new JLabel("Not Signed In");
        userNameLabel.setFont(userNameLabel.getFont().deriveFont(Font.BOLD, 13f));

        userRoleBadge = new JLabel("GUEST");
        userRoleBadge.setFont(userRoleBadge.getFont().deriveFont(Font.PLAIN, 11f));
        userRoleBadge.setForeground(new Color(37, 99, 235));

        userInfoPanel.add(userNameLabel);
        userInfoPanel.add(userRoleBadge);
        footerPanel.add(userInfoPanel);
        footerPanel.add(Box.createVerticalStrut(12));

        JButton logoutBtn = new JButton("🚪  Sign Out");
        logoutBtn.putClientProperty("JButton.buttonType", "roundRect");
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        logoutBtn.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        footerPanel.add(logoutBtn);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private void addNavButton(JPanel container, String text, String navKey, boolean visible) {
        if (!visible) return;

        JButton btn = new JButton(text);
        btn.putClientProperty("navKey", navKey);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setPreferredSize(new Dimension(216, 38));
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 13f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);

        updateButtonStyling(btn, navKey.equals(activeNavKey));

        btn.addActionListener(e -> {
            setActiveNavKey(navKey);
            if (onNavigate != null) {
                onNavigate.accept(navKey);
            }
        });

        navButtons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(4));
    }

    public void setActiveNavKey(String navKey) {
        this.activeNavKey = navKey;
        for (JButton b : navButtons) {
            String key = (String) b.getClientProperty("navKey");
            updateButtonStyling(b, key != null && key.equals(navKey));
        }
    }

    private void updateButtonStyling(JButton button, boolean active) {
        if (active) {
            button.setBackground(new Color(224, 231, 255)); // light indigo highlight
            button.setForeground(new Color(67, 56, 202));
            button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        } else {
            button.setBackground(new Color(248, 250, 252));
            button.setForeground(new Color(51, 65, 85));
            button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
        }
    }

    public void updateUserDisplay() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userRoleBadge.setText(user.getRole().getDisplayName().toUpperCase());
        } else {
            userNameLabel.setText("Signed Out");
            userRoleBadge.setText("NO ACTIVE SESSION");
        }
    }
}
