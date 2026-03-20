package com.mod.archetype.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ClassScreenRenderer {

    public static void renderSectionHeader(GuiGraphics g, Font font, String titleKey, int x, int y, int width) {
        Component title = Component.translatable(titleKey);
        int textWidth = font.width(title);
        int lineY = y + font.lineHeight / 2;
        int pad = 4;

        int centerX = x + width / 2;
        int textX = centerX - textWidth / 2;

        g.fill(x, lineY, textX - pad, lineY + 1, 0x40FFFFFF);
        g.drawString(font, title, textX, y, 0x888888, false);
        g.fill(textX + textWidth + pad, lineY, x + width, lineY + 1, 0x40FFFFFF);
    }

    public static void renderAttributeBar(GuiGraphics g, Font font, String name, double value, double baseValue,
                                           int x, int y, int barWidth) {
        int barHeight = 6;
        int nameWidth = 80;
        int barX = x + nameWidth + 4;

        g.drawString(font, name, x, y, 0xCCCCCC, false);

        // Background
        g.fill(barX, y + 2, barX + barWidth, y + 2 + barHeight, 0x40FFFFFF);

        // Fill based on ratio to base
        double ratio = baseValue != 0 ? value / baseValue : 1.0;
        int fillWidth = (int) Math.min(barWidth, barWidth * (ratio / 2.0));
        int barColor = value > baseValue ? 0xFF44CC44 : (value < baseValue ? 0xFFCC4444 : 0xFFCCCCCC);
        g.fill(barX, y + 2, barX + fillWidth, y + 2 + barHeight, barColor);

        // Value text
        String diff = value > baseValue ? "+" + formatValue(value - baseValue) : (value < baseValue ? formatValue(value - baseValue) : "");
        String text = formatValue(value) + (diff.isEmpty() ? "" : " (" + diff + ")");
        g.drawString(font, text, barX + barWidth + 4, y, barColor, false);
    }

    public static void renderProgressBar(GuiGraphics g, int x, int y, int width, int height,
                                          float progress, int color) {
        g.fill(x, y, x + width, y + height, 0x25FFFFFF);
        int fillWidth = (int) (width * Math.max(0, Math.min(1, progress)));
        g.fill(x, y, x + fillWidth, y + height, 0xFF000000 | color);
    }

    private static String formatValue(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }
}
