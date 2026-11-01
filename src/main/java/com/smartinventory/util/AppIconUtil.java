package com.smartinventory.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for loading and applying the official Smart Inventory application branding icon
 * across Swing windows, dialogs, taskbar, and UI components.
 */
public final class AppIconUtil {

    private static final Logger logger = LoggerFactory.getLogger(AppIconUtil.class);
    private static final String ICON_RESOURCE_PATH = "/icons/app-icon.png";

    private static BufferedImage baseImage;
    private static List<Image> multiResolutionIcons;

    static {
        loadIcon();
    }

    private AppIconUtil() {
    }

    private static synchronized void loadIcon() {
        try {
            URL url = AppIconUtil.class.getResource(ICON_RESOURCE_PATH);
            if (url != null) {
                baseImage = ImageIO.read(url);
            } else {
                // Fallback using InputStream directly from ClassLoader
                try (InputStream is = AppIconUtil.class.getResourceAsStream(ICON_RESOURCE_PATH)) {
                    if (is != null) {
                        baseImage = ImageIO.read(is);
                    }
                }
            }

            if (baseImage != null) {
                logger.info("Successfully loaded application icon from '{}' ({}x{})",
                        ICON_RESOURCE_PATH, baseImage.getWidth(), baseImage.getHeight());
                multiResolutionIcons = createMultiResolutionList(baseImage);
            } else {
                logger.warn("Application icon resource not found at '{}'", ICON_RESOURCE_PATH);
                multiResolutionIcons = Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Failed to load application branding icon: {}", e.getMessage(), e);
            multiResolutionIcons = Collections.emptyList();
        }
    }

    private static List<Image> createMultiResolutionList(BufferedImage source) {
        int[] targetSizes = {16, 24, 32, 48, 64, 128, 256, 512};
        List<Image> list = new ArrayList<>();

        for (int size : targetSizes) {
            if (source.getWidth() >= size && source.getHeight() >= size) {
                list.add(createHighQualityScaledImage(source, size, size));
            }
        }
        // Always include the original master image
        list.add(source);
        return Collections.unmodifiableList(list);
    }

    /**
     * Produces an anti-aliased, bicubic-filtered scaled version of the given icon.
     */
    private static BufferedImage createHighQualityScaledImage(BufferedImage src, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, width, height, null);
        g2.dispose();
        return resized;
    }

    /**
     * Returns the master branding Image, or null if loading failed.
     */
    public static Image getAppImage() {
        return baseImage;
    }

    /**
     * Returns a list of multi-resolution images suitable for {@link Window#setIconImages(List)}.
     * This provides Windows with crisp icons at 16x16, 32x32, 48x48, 64x64, 128x128, and 256x256.
     */
    public static List<Image> getAppIconImages() {
        return multiResolutionIcons;
    }

    /**
     * Sets the application icon on the given Swing Window (JFrame, JDialog).
     */
    public static void applyToWindow(Window window) {
        if (window == null) return;
        if (multiResolutionIcons != null && !multiResolutionIcons.isEmpty()) {
            window.setIconImages(multiResolutionIcons);
        } else if (baseImage != null) {
            window.setIconImage(baseImage);
        }
    }

    /**
     * Configures the operating system taskbar/dock icon on platforms that support java.awt.Taskbar.
     */
    public static void applyToTaskbar() {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE) && baseImage != null) {
                    taskbar.setIconImage(baseImage);
                    logger.debug("Successfully applied branding icon to system taskbar.");
                }
            }
        } catch (Throwable t) {
            logger.debug("System taskbar icon configuration skipped or unsupported: {}", t.getMessage());
        }
    }

    /**
     * Returns an ImageIcon scaled to specific dimensions with smooth anti-aliased scaling.
     */
    public static ImageIcon getScaledIcon(int width, int height) {
        if (baseImage == null) {
            return null;
        }
        return new ImageIcon(createHighQualityScaledImage(baseImage, width, height));
    }
}
