package com.smartinventory.model.enums;

/**
 * Enumeration of immutable stock transaction movement types.
 */
public enum TransactionType {
    PURCHASE_IN("Purchase In", true),
    RETURN_IN("Return In", true),
    SALE_OUT("Sale Out", false),
    DAMAGE_OUT("Damage Out", false),
    EXPIRED_WRITE_OFF("Expired Write-Off", false);

    private final String displayName;
    private final boolean incoming;

    TransactionType(String displayName, boolean incoming) {
        this.displayName = displayName;
        this.incoming = incoming;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isIncoming() {
        return incoming;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
