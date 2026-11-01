package com.smartinventory.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests verifying that the application branding icon loads correctly
 * from Maven resources and generates multi-resolution icon sets for Windows.
 */
public class AppIconUtilTest {

    @Test
    @DisplayName("AppIcon: Should load base image from resources/icons/app-icon.png")
    public void testGetAppImage() {
        Image img = AppIconUtil.getAppImage();
        assertNotNull(img, "Application base branding image must not be null");
        assertTrue(img.getWidth(null) > 0, "Icon width must be greater than 0");
        assertTrue(img.getHeight(null) > 0, "Icon height must be greater than 0");
    }

    @Test
    @DisplayName("AppIcon: Should produce multi-resolution icon list for Windows OS")
    public void testGetAppIconImages() {
        List<Image> list = AppIconUtil.getAppIconImages();
        assertNotNull(list, "Multi-resolution icon list must not be null");
        assertFalse(list.isEmpty(), "Multi-resolution icon list must contain icon sizes");
        assertTrue(list.size() >= 4, "Should provide at least 4 resolutions for crisp scaling");
    }

    @Test
    @DisplayName("AppIcon: Should generate high-quality scaled ImageIcon for UI headers")
    public void testGetScaledIcon() {
        ImageIcon icon32 = AppIconUtil.getScaledIcon(32, 32);
        assertNotNull(icon32, "Scaled 32x32 icon must not be null");
        assertEquals(32, icon32.getIconWidth(), "Width must match requested dimension");
        assertEquals(32, icon32.getIconHeight(), "Height must match requested dimension");

        ImageIcon icon64 = AppIconUtil.getScaledIcon(64, 64);
        assertNotNull(icon64, "Scaled 64x64 icon must not be null");
        assertEquals(64, icon64.getIconWidth());
        assertEquals(64, icon64.getIconHeight());
    }

    @Test
    @DisplayName("AppIcon: Should safely apply icon to Swing Window without throwing exception")
    public void testApplyToWindow() {
        JFrame frame = new JFrame("Test Window");
        assertDoesNotThrow(() -> AppIconUtil.applyToWindow(frame));
        List<Image> icons = frame.getIconImages();
        assertNotNull(icons, "Window icon images must be set");
        assertFalse(icons.isEmpty(), "Window should receive the icon list");
        frame.dispose();
    }

    @Test
    @DisplayName("AppIcon: Should execute taskbar branding without throwing exception")
    public void testApplyToTaskbar() {
        assertDoesNotThrow(AppIconUtil::applyToTaskbar);
    }
}
