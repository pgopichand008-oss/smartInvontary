package com.smartinventory.view.components;

import com.smartinventory.model.enums.ExpirySeverity;
import com.smartinventory.model.enums.StockAlertSeverity;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern pill-style cell renderer for Expiry and Stock Deficit alert severities.
 */
public class AlertBadgeRenderer extends DefaultTableCellRenderer {

    // Severity Colors
    private static final Color CRITICAL_BG = new Color(254, 226, 226);
    private static final Color CRITICAL_FG = new Color(185, 28, 28);

    private static final Color HIGH_BG = new Color(254, 240, 138);
    private static final Color HIGH_FG = new Color(133, 77, 14);

    private static final Color MEDIUM_BG = new Color(254, 249, 195);
    private static final Color MEDIUM_FG = new Color(161, 98, 7);

    private static final Color OUT_OF_STOCK_BG = new Color(255, 228, 230);
    private static final Color OUT_OF_STOCK_FG = new Color(190, 18, 60);

    private static final Color LOW_STOCK_BG = new Color(254, 243, 199);
    private static final Color LOW_STOCK_FG = new Color(180, 83, 9);

    public AlertBadgeRenderer() {
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
        Color bg = CRITICAL_BG;
        Color fg = CRITICAL_FG;
        String text = value.toString();

        if (value instanceof ExpirySeverity) {
            ExpirySeverity severity = (ExpirySeverity) value;
            text = severity.getDisplayName();
            switch (severity) {
                case EXPIRED:
                case EXPIRING_7_DAYS:
                    bg = CRITICAL_BG; fg = CRITICAL_FG; break;
                case EXPIRING_15_DAYS:
                    bg = HIGH_BG; fg = HIGH_FG; break;
                case EXPIRING_30_DAYS:
                    bg = MEDIUM_BG; fg = MEDIUM_FG; break;
            }
        } else if (value instanceof StockAlertSeverity) {
            StockAlertSeverity severity = (StockAlertSeverity) value;
            text = severity.getDisplayName();
            if (severity == StockAlertSeverity.OUT_OF_STOCK) {
                bg = OUT_OF_STOCK_BG; fg = OUT_OF_STOCK_FG;
            } else {
                bg = LOW_STOCK_BG; fg = LOW_STOCK_FG;
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
