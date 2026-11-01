package com.smartinventory.view.dashboard;

import com.smartinventory.model.dashboard.CategoryStockStat;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Graphics2D Donut Chart displaying stock distribution across product categories.
 */
public class CategoryDistributionChartPanel extends JPanel {

    private static final Color[] PALETTE = {
            new Color(37, 99, 235),   // Royal Blue
            new Color(16, 185, 129),  // Emerald Green
            new Color(245, 158, 11),  // Amber
            new Color(139, 92, 246),  // Purple
            new Color(6, 182, 212),   // Cyan
            new Color(236, 72, 153),  // Pink
            new Color(100, 116, 139)  // Slate
    };

    private List<CategoryStockStat> stats = new ArrayList<>();

    public CategoryDistributionChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(360, 240));
        setMinimumSize(new Dimension(280, 200));
    }

    public void setData(List<CategoryStockStat> stats) {
        this.stats = stats != null ? stats : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (stats.isEmpty() || stats.stream().mapToInt(CategoryStockStat::getTotalUnits).sum() == 0) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String msg = "No category inventory data available";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2);
            g2.dispose();
            return;
        }

        int totalUnits = stats.stream().mapToInt(CategoryStockStat::getTotalUnits).sum();

        // Chart area on left, legend on right
        int chartSize = Math.min(height - 40, (width / 2) - 20);
        chartSize = Math.max(chartSize, 80);
        int chartX = 25;
        int chartY = (height - chartSize) / 2;

        // Draw donut slices
        double currentAngle = 90.0;
        for (int i = 0; i < stats.size(); i++) {
            CategoryStockStat stat = stats.get(i);
            double angle = (stat.getTotalUnits() / (double) totalUnits) * 360.0;
            Color sliceColor = PALETTE[i % PALETTE.length];

            g2.setColor(sliceColor);
            g2.fillArc(chartX, chartY, chartSize, chartSize, (int) Math.round(currentAngle), (int) Math.ceil(angle));
            currentAngle += angle;
        }

        // Donut inner hole
        int holeSize = (int) (chartSize * 0.58);
        int holeX = chartX + (chartSize - holeSize) / 2;
        int holeY = chartY + (chartSize - holeSize) / 2;
        g2.setColor(getBackground());
        g2.fillOval(holeX, holeY, holeSize, holeSize);

        // Center label (Total Units)
        g2.setColor(new Color(15, 23, 42));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        String totalStr = String.valueOf(totalUnits);
        FontMetrics fmTotal = g2.getFontMetrics();
        g2.drawString(totalStr, holeX + (holeSize - fmTotal.stringWidth(totalStr)) / 2, holeY + (holeSize / 2) + 2);

        g2.setColor(new Color(100, 116, 139));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        String subLabel = "Total Units";
        FontMetrics fmSub = g2.getFontMetrics();
        g2.drawString(subLabel, holeX + (holeSize - fmSub.stringWidth(subLabel)) / 2, holeY + (holeSize / 2) + 16);

        // Draw Legend on right
        int legendX = chartX + chartSize + 25;
        int legendY = 25;
        int legendRowH = 24;

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fmLG = g2.getFontMetrics();

        for (int i = 0; i < stats.size() && i < 6; i++) {
            CategoryStockStat stat = stats.get(i);
            Color dotColor = PALETTE[i % PALETTE.length];

            // Color badge
            g2.setColor(dotColor);
            g2.fillRoundRect(legendX, legendY + 4, 10, 10, 3, 3);

            // Name
            g2.setColor(new Color(51, 65, 85));
            String name = stat.getCategoryName();
            if (fmLG.stringWidth(name) > 110) {
                while (fmLG.stringWidth(name + "..") > 110 && name.length() > 3) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "..";
            }
            g2.drawString(name, legendX + 16, legendY + 13);

            // Count and percentage
            g2.setColor(new Color(100, 116, 139));
            String meta = String.format("%d (%.1f%%)", stat.getTotalUnits(), stat.getPercentage());
            g2.drawString(meta, legendX + 135, legendY + 13);

            legendY += legendRowH;
        }

        if (stats.size() > 6) {
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("+ " + (stats.size() - 6) + " more categories", legendX + 16, legendY + 10);
        }

        g2.dispose();
    }
}
