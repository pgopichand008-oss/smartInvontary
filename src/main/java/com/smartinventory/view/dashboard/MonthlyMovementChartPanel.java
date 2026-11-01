package com.smartinventory.view.dashboard;

import com.smartinventory.model.dashboard.MonthlyMovementStat;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Graphics2D Grouped Bar Chart displaying inventory movement trends across recent months.
 */
public class MonthlyMovementChartPanel extends JPanel {

    private static final Color INFLOW_COLOR = new Color(16, 185, 129);   // Emerald Green
    private static final Color OUTFLOW_COLOR = new Color(37, 99, 235);   // Royal Blue
    private static final Color DISCARD_COLOR = new Color(239, 68, 68);   // Red
    private static final Color GRID_COLOR = new Color(241, 245, 249);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);

    private List<MonthlyMovementStat> stats = new ArrayList<>();

    public MonthlyMovementChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(460, 240));
        setMinimumSize(new Dimension(320, 200));
    }

    public void setData(List<MonthlyMovementStat> stats) {
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

        // Draw top legend
        int legendX = 40;
        int legendY = 16;
        drawLegendItem(g2, legendX, legendY, INFLOW_COLOR, "Inflow (Stock-In)");
        drawLegendItem(g2, legendX + 130, legendY, OUTFLOW_COLOR, "Outflow (Dispense)");
        drawLegendItem(g2, legendX + 265, legendY, DISCARD_COLOR, "Discards / Expired");

        if (stats.isEmpty()) {
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String msg = "No historical stock movement recorded";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2 + 10);
            g2.dispose();
            return;
        }

        int padLeft = 45;
        int padRight = 20;
        int padTop = 40;
        int padBottom = 35;

        int plotW = width - padLeft - padRight;
        int plotH = height - padTop - padBottom;

        // Determine max Y value
        int maxVal = 10;
        for (MonthlyMovementStat s : stats) {
            maxVal = Math.max(maxVal, s.getInflow());
            maxVal = Math.max(maxVal, s.getOutflow());
            maxVal = Math.max(maxVal, s.getDiscards());
        }
        // Round up maxVal to nice step
        int yMax = ((maxVal + 9) / 10) * 10;
        if (yMax < 10) yMax = 10;

        // Draw horizontal grid lines & Y labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        FontMetrics fmY = g2.getFontMetrics();
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int val = (yMax * i) / gridSteps;
            int y = padTop + plotH - (int) ((val / (double) yMax) * plotH);

            g2.setColor(GRID_COLOR);
            g2.drawLine(padLeft, y, width - padRight, y);

            g2.setColor(TEXT_MUTED);
            String valStr = String.valueOf(val);
            g2.drawString(valStr, padLeft - fmY.stringWidth(valStr) - 6, y + 4);
        }

        // Draw grouped bars
        int numGroups = stats.size();
        int groupWidth = plotW / Math.max(1, numGroups);
        int barW = Math.max(6, Math.min(16, (groupWidth - 20) / 3));

        for (int i = 0; i < numGroups; i++) {
            MonthlyMovementStat s = stats.get(i);
            int groupCenterX = padLeft + (i * groupWidth) + (groupWidth / 2);

            int startX = groupCenterX - (int) (barW * 1.6);

            // Inflow Bar
            drawBar(g2, startX, padTop, plotH, yMax, s.getInflow(), INFLOW_COLOR, barW);
            // Outflow Bar
            drawBar(g2, startX + barW + 2, padTop, plotH, yMax, s.getOutflow(), OUTFLOW_COLOR, barW);
            // Discard Bar
            drawBar(g2, startX + (barW + 2) * 2, padTop, plotH, yMax, s.getDiscards(), DISCARD_COLOR, barW);

            // X-axis label (month)
            g2.setColor(new Color(71, 85, 105));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String label = s.getMonth();
            FontMetrics fmLbl = g2.getFontMetrics();
            g2.drawString(label, groupCenterX - (fmLbl.stringWidth(label) / 2), height - 12);
        }

        g2.dispose();
    }

    private void drawBar(Graphics2D g2, int x, int padTop, int plotH, int yMax, int val, Color color, int barW) {
        if (val <= 0) return;
        int barH = (int) Math.round((val / (double) yMax) * plotH);
        barH = Math.max(2, Math.min(plotH, barH));
        int barY = padTop + plotH - barH;

        g2.setColor(color);
        g2.fillRoundRect(x, barY, barW, barH, 4, 4);
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, 10, 10, 3, 3);
        g2.setColor(new Color(71, 85, 105));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(label, x + 15, y + 9);
    }
}
