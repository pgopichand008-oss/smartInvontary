package com.smartinventory.view.components;

import com.smartinventory.model.enums.BatchStatus;
import com.smartinventory.model.enums.TransactionType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern pill-style cell renderer for BatchStatus and TransactionType enums.
 */
public class InventoryStatusBadgeRenderer extends DefaultTableCellRenderer {

    // Status Colors
    private static final Color ACTIVE_BG = new Color(220, 252, 231);
    private static final Color ACTIVE_FG = new Color(22, 101, 52);

    private static final Color EXPIRING_BG = new Color(254, 240, 138);
    private static final Color EXPIRING_FG = new Color(133, 77, 14);

    private static final Color EXPIRED_BG = new Color(254, 226, 226);
    private static final Color EXPIRED_FG = new Color(185, 28, 28);

    private static final Color DEPLETED_BG = new Color(241, 245, 249);
    private static final Color DEPLETED_FG = new Color(100, 116, 139);

    // Movement Colors
    private static final Color INCOMING_BG = new Color(220, 252, 231);
    private static final Color INCOMING_FG = new Color(22, 101, 52);

    private static final Color SALE_BG = new Color(224, 231, 255);
    private static final Color SALE_FG = new Color(55, 48, 163);

    private static final Color WRITE_OFF_BG = new Color(254, 226, 226);
    private static final Color WRITE_OFF_FG = new Color(185, 28, 28);

    public InventoryStatusBadgeRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value == null) {
            return label;
        }

        label.setOpaque(true);
        Color bg = DEPLETED_BG;
        Color fg = DEPLETED_FG;
        String text = value.toString();

        if (value instanceof BatchStatus) {
            BatchStatus status = (BatchStatus) value;
            text = status.getDisplayName();
            switch (status) {
                case ACTIVE:
                    bg = ACTIVE_BG; fg = ACTIVE_FG; break;
                case EXPIRING_SOON:
                    bg = EXPIRING_BG; fg = EXPIRING_FG; break;
                case EXPIRED:
                    bg = EXPIRED_BG; fg = EXPIRED_FG; break;
                case DEPLETED:
                    bg = DEPLETED_BG; fg = DEPLETED_FG; break;
            }
        } else if (value instanceof TransactionType) {
            TransactionType type = (TransactionType) value;
            text = type.getDisplayName();
            switch (type) {
                case PURCHASE_IN:
                case RETURN_IN:
                    bg = INCOMING_BG; fg = INCOMING_FG; break;
                case SALE_OUT:
                    bg = SALE_BG; fg = SALE_FG; break;
                case DAMAGE_OUT:
                case EXPIRED_WRITE_OFF:
                    bg = WRITE_OFF_BG; fg = WRITE_OFF_FG; break;
            }
        }

        label.setText(text);

        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(bg);
            label.setForeground(fg);
        }

        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return label;
    }
}
