package com.smartinventory.view.auth;

import com.smartinventory.controller.AuthController;
import com.smartinventory.model.User;
import com.smartinventory.util.AppIconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * Modern FlatLaf-styled Login Dialog.
 */
public class LoginDialog extends JDialog {

    private final AuthController authController;
    private final Consumer<User> onLoginSuccess;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton signInButton;
    private JLabel errorLabel;
    private boolean authenticated = false;

    public LoginDialog(Frame owner, AuthController authController, Consumer<User> onLoginSuccess) {
        super(owner, "Sign In - Smart Inventory & Expiry Management", true);
        this.authController = authController;
        this.onLoginSuccess = onLoginSuccess;

        AppIconUtil.applyToWindow(this);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (!authenticated) {
                    System.exit(0);
                }
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        mainPanel.setPreferredSize(new Dimension(380, 420));

        // --- Brand Header ---
        ImageIcon logoIcon = AppIconUtil.getScaledIcon(64, 64);
        JLabel brandIconLabel = logoIcon != null ? new JLabel(logoIcon) : new JLabel("📦");
        if (logoIcon == null) {
            brandIconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        }
        brandIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Smart Inventory");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Sign in to your account to continue");
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(brandIconLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(24));

        // --- Form Fields ---
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD, 12f));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel userLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        userLabelPanel.setMaximumSize(new Dimension(310, 20));
        userLabelPanel.add(userLabel);
        mainPanel.add(userLabelPanel);
        mainPanel.add(Box.createVerticalStrut(4));

        usernameField = new JTextField(20);
        usernameField.putClientProperty("JTextField.placeholderText", "Enter your username");
        usernameField.setMaximumSize(new Dimension(310, 36));
        usernameField.setPreferredSize(new Dimension(310, 36));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(14));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(passLabel.getFont().deriveFont(Font.BOLD, 12f));
        JPanel passLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        passLabelPanel.setMaximumSize(new Dimension(310, 20));
        passLabelPanel.add(passLabel);
        mainPanel.add(passLabelPanel);
        mainPanel.add(Box.createVerticalStrut(4));

        passwordField = new JPasswordField(20);
        passwordField.putClientProperty("JTextField.placeholderText", "Enter your password");
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);
        passwordField.setMaximumSize(new Dimension(310, 36));
        passwordField.setPreferredSize(new Dimension(310, 36));
        mainPanel.add(passwordField);

        // Enter key listeners
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        usernameField.addKeyListener(enterListener);
        passwordField.addKeyListener(enterListener);

        // Error message label
        mainPanel.add(Box.createVerticalStrut(8));
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createVerticalStrut(12));

        // Sign In Button
        signInButton = new JButton("Sign In");
        signInButton.putClientProperty("JButton.buttonType", "roundRect");
        signInButton.setBackground(new Color(37, 99, 235));
        signInButton.setForeground(Color.WHITE);
        signInButton.setFont(signInButton.getFont().deriveFont(Font.BOLD, 13f));
        signInButton.setMaximumSize(new Dimension(310, 38));
        signInButton.setPreferredSize(new Dimension(310, 38));
        signInButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signInButton.addActionListener(e -> performLogin());
        mainPanel.add(signInButton);

        // Footer Hint
        mainPanel.add(Box.createVerticalGlue());
        JLabel hintLabel = new JLabel("<html><center><font color='#94a3b8'>Default Admin: <b>admin</b> / <b>admin123</b></font></center></html>");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 11f));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(hintLabel);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(signInButton);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty()) {
            errorLabel.setText("Please enter your username.");
            usernameField.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please enter your password.");
            passwordField.requestFocusInWindow();
            return;
        }

        // Set UI to loading state
        setLoadingState(true);
        errorLabel.setText("Authenticating...");
        errorLabel.setForeground(new Color(100, 116, 139));

        authController.handleLogin(
                username,
                password,
                user -> {
                    authenticated = true;
                    setLoadingState(false);
                    dispose();
                    if (onLoginSuccess != null) {
                        onLoginSuccess.accept(user);
                    }
                },
                errorMessage -> {
                    setLoadingState(false);
                    errorLabel.setForeground(new Color(220, 38, 38));
                    errorLabel.setText(errorMessage);
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                }
        );
    }

    private void setLoadingState(boolean loading) {
        signInButton.setEnabled(!loading);
        signInButton.setText(loading ? "Signing In..." : "Sign In");
        usernameField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
