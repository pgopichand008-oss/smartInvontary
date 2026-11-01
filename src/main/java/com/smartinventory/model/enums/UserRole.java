package com.smartinventory.model.enums;

/**
 * Roles defining permissions and access levels in the application.
 */
public enum UserRole {
    ADMIN("Administrator", "Full system access including user and system configuration"),
    MANAGER("Store Manager", "Inventory, suppliers, products, and reporting access"),
    CASHIER("Cashier", "Sales operations and stock lookup access");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromString(String roleStr) {
        if (roleStr == null) return CASHIER;
        try {
            return UserRole.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return CASHIER;
        }
    }
}
