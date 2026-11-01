package com.smartinventory.view.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern pill-style cell renderer for boolean or enum status badges.
 */
public class StatusBadgeRenderer extends DefaultTableCellRenderer {

    private final Color activeBg = new Color(220, 252, 231);
    private final Color activeFg = new Color(22, 101, 52);
    private final Color inactiveBg = new Color(241, 245, 249);
    private final Color inactiveFg = new Color(100, 116, 139);

    public StatusBadgeRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        boolean isActive = false;
        if (value instanceof Boolean) {
            isActive = (Boolean) value;
            label.setText(isActive ? "Active" : "Inactive");
        } else if (value != null) {
            isActive = "Active".equalsIgnoreCase(value.toString()) || "TRUE".equalsIgnoreCase(value.toString());
            label.setText(value.toString());
        }

        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(isActive ? activeBg : inactiveBg);
            label.setForeground(isActive ? activeFg : inactiveFg);
        }

        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        return label;
    }
}
