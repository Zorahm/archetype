package com.mod.archetype.gui;

import com.mod.archetype.core.ClassCategory;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ClassSelectionScreen extends Screen {

    private final int mode; // 0=first, 1=rebirth
    private List<PlayerClass> allClasses;
    private List<PlayerClass> filteredClasses;
    private final Set<ClassCategory> activeFilters = new HashSet<>();
    private int hoveredIndex = -1;
    private int selectedIndex = -1;
    private float[] cardScales;
    private int scrollOffset = 0;
    @Nullable
    private PlayerClass hoveredClass;

    private int gridCols, gridRows, cardSize, gridStartX, gridStartY, cardSpacing;

    // Layout
    private static final int LORE_PANEL_HEIGHT = 48;
    private static final int TITLE_HEIGHT = 28;

    public ClassSelectionScreen(int mode) {
        super(Component.translatable(mode == 0 ? "gui.archetype.selection.title" : "gui.archetype.rebirth.title"));
        this.mode = mode;
    }

    @Override
    protected void init() {
        allClasses = new ArrayList<>(ClassRegistry.getInstance().getAll());
        allClasses.sort(Comparator.comparing(c -> c.getId().toString()));
        filteredClasses = new ArrayList<>(allClasses);
        recalculateLayout();
        cardScales = new float[filteredClasses.size()];
        Arrays.fill(cardScales, 1.0f);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark vignette background
        renderDimBackground(graphics);

        // Title area
        renderTitle(graphics);

        // Cards
        hoveredIndex = -1;
        hoveredClass = null;
        for (int i = 0; i < filteredClasses.size(); i++) {
            int row = i / gridCols;
            int col = i % gridCols;
            int x = gridStartX + col * (cardSize + cardSpacing);
            int y = gridStartY + row * (cardSize + cardSpacing + 14) - scrollOffset;

            if (y + cardSize < TITLE_HEIGHT || y > height - LORE_PANEL_HEIGHT - 10) continue;

            PlayerClass cls = filteredClasses.get(i);
            boolean hovered = mouseX >= x && mouseX <= x + cardSize && mouseY >= y && mouseY <= y + cardSize;
            if (hovered) {
                hoveredIndex = i;
                hoveredClass = cls;
            }

            float targetScale = hovered ? 1.08f : 1.0f;
            if (i < cardScales.length) {
                cardScales[i] = Mth.lerp(0.25f, cardScales[i], targetScale);
            }

            boolean isCurrent = mode == 1 && ClientClassData.getInstance().hasClass()
                    && cls.getId().equals(ClientClassData.getInstance().getClassId());
            boolean isSelected = i == selectedIndex;
            renderCard(graphics, cls, x, y, cardSize, i < cardScales.length ? cardScales[i] : 1.0f, hovered, isCurrent, isSelected);
        }

        // Bottom lore panel
        renderLorePanel(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDimBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    private void renderTitle(GuiGraphics g) {
        // Title text
        int titleY = 8;
        g.drawCenteredString(font, title, width / 2, titleY, 0xDDDDDD);

        // Decorative lines on sides of title
        int tw = font.width(title);
        int lineY = titleY + font.lineHeight / 2;
        int pad = 8;
        int lineLen = 60;
        // Left line
        g.fill(width / 2 - tw / 2 - pad - lineLen, lineY, width / 2 - tw / 2 - pad, lineY + 1, 0x25FFFFFF);
        // Right line
        g.fill(width / 2 + tw / 2 + pad, lineY, width / 2 + tw / 2 + pad + lineLen, lineY + 1, 0x25FFFFFF);

        // Subtle separator under title
        g.fill(width / 4, TITLE_HEIGHT - 2, width * 3 / 4, TITLE_HEIGHT - 1, 0x12FFFFFF);
    }

    private void renderCard(GuiGraphics g, PlayerClass cls, int x, int y, int size, float scale,
                            boolean hovered, boolean isCurrent, boolean isSelected) {
        int classColor = cls.getColor();
        var pose = g.pose();
        boolean transformed = scale != 1.0f;
        if (transformed) {
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            pose.pushPose();
            pose.translate(cx, cy, 0);
            pose.scale(scale, scale, 1);
            pose.translate(-cx, -cy, 0);
        }

        // Card background — gradient from dark to slightly lighter
        int bgTop = hovered ? 0xFF2C2C38 : 0xFF1E1E28;
        int bgBot = hovered ? 0xFF262632 : 0xFF181822;
        g.fill(x, y, x + size, y + size / 2, bgTop);
        g.fill(x, y + size / 2, x + size, y + size, bgBot);

        // Bottom color accent strip
        int accentAlpha = hovered ? 0xFF : 0x90;
        g.fill(x, y + size - 3, x + size, y + size, (accentAlpha << 24) | classColor);

        // Border (2px thick)
        int borderAlpha = hovered ? 0xFF : (isSelected ? 0xCC : 0x80);
        int borderColor = (borderAlpha << 24) | classColor;
        // Top
        g.fill(x, y, x + size, y + 2, borderColor);
        // Bottom
        g.fill(x, y + size - 2, x + size, y + size, borderColor);
        // Left
        g.fill(x, y, x + 2, y + size, borderColor);
        // Right
        g.fill(x + size - 2, y, x + size, y + size, borderColor);

        // Current class indicator (for rebirth mode)
        if (isCurrent) {
            // Small diamond/marker in top-right corner
            g.fill(x + size - 8, y + 2, x + size - 2, y + 8, 0xFF000000 | classColor);
        }

        // Keyboard selection indicator
        if (isSelected && !hovered) {
            // Pulsing outer glow (simplified as brighter outline)
            g.fill(x - 1, y - 1, x + size + 1, y, 0x80000000 | classColor);
            g.fill(x - 1, y + size, x + size + 1, y + size + 1, 0x80000000 | classColor);
            g.fill(x - 1, y, x, y + size, 0x80000000 | classColor);
            g.fill(x + size, y, x + size + 1, y + size, 0x80000000 | classColor);
        }

        // Name below card
        Component name = Component.translatable(cls.getNameKey());
        int nameColor = hovered ? (0xFF000000 | classColor) : 0xFFBBBBBB;
        g.drawCenteredString(font, name, x + size / 2, y + size + 3, nameColor);

        if (transformed) {
            pose.popPose();
        }
    }

    private void renderLorePanel(GuiGraphics g, int mouseX, int mouseY) {
        int panelY = height - LORE_PANEL_HEIGHT;

        // Panel background
        g.fill(0, panelY, width, height, 0xFF181824);
        // Top border
        g.fill(0, panelY, width, panelY + 1, 0x20FFFFFF);

        if (hoveredClass != null) {
            int classColor = hoveredClass.getColor();

            // Class name on the left
            Component name = Component.translatable(hoveredClass.getNameKey());
            g.drawString(font, name, 12, panelY + 6, 0xFF000000 | classColor, false);

            // Lore text (word-wrapped, right of name or below on narrow screens)
            List<String> loreKeys = hoveredClass.getLoreKeys();
            if (!loreKeys.isEmpty()) {
                Component lore = Component.translatable(loreKeys.get(0))
                        .withStyle(s -> s.withItalic(true).withColor(0x888888));
                int loreX = 12;
                int loreY = panelY + 18;
                int loreWidth = width - 24;
                List<FormattedCharSequence> lines = font.split(lore, loreWidth);
                for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                    g.drawString(font, lines.get(i), loreX, loreY, 0x888888, false);
                    loreY += font.lineHeight + 1;
                }
            }

            // "Click to view" hint on the right
            Component hint = Component.translatable("gui.archetype.click_to_view")
                    .withStyle(Style.EMPTY.withColor(0x555555));
            // Fall back if key doesn't exist — just show nothing extra
            int hintW = font.width(hint);
            g.drawString(font, hint, width - hintW - 12, panelY + 6, 0x555555, false);
        } else {
            // No hover — show general hint
            Component hint = Component.translatable("gui.archetype.hover_hint")
                    .withStyle(Style.EMPTY.withColor(0x555555));
            g.drawCenteredString(font, hint, width / 2, panelY + (LORE_PANEL_HEIGHT - font.lineHeight) / 2, 0x555555);
        }

        // Random button area (bottom-right, custom rendered)
        renderRandomButton(g, mouseX, mouseY);
    }

    private void renderRandomButton(GuiGraphics g, int mouseX, int mouseY) {
        Component text = Component.translatable("gui.archetype.random");
        int bw = font.width(text) + 16;
        int bh = 16;
        int bx = width - bw - 8;
        int by = height - bh - 6;
        boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;

        g.fill(bx, by, bx + bw, by + bh, hovered ? 0x40FFFFFF : 0x18FFFFFF);
        g.fill(bx, by, bx + bw, by + 1, hovered ? 0x35FFFFFF : 0x12FFFFFF);
        g.drawCenteredString(font, text, bx + bw / 2, by + (bh - font.lineHeight) / 2, hovered ? 0xFFFFFF : 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Random button
            Component text = Component.translatable("gui.archetype.random");
            int bw = font.width(text) + 16;
            int bh = 16;
            int bx = width - bw - 8;
            int by = height - bh - 6;
            if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                if (!filteredClasses.isEmpty()) {
                    PlayerClass randomClass = filteredClasses.get(new Random().nextInt(filteredClasses.size()));
                    Minecraft.getInstance().setScreen(new ClassDetailScreen(randomClass, mode));
                }
                return true;
            }

            // Card click
            if (hoveredIndex >= 0 && hoveredIndex < filteredClasses.size()) {
                Minecraft.getInstance().setScreen(new ClassDetailScreen(filteredClasses.get(hoveredIndex), mode));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && mode == 0) {
            return true; // ESC blocked on first selection
        }
        if (keyCode == 265) { // UP
            selectedIndex = Math.max(0, selectedIndex - gridCols);
            return true;
        }
        if (keyCode == 264) { // DOWN
            selectedIndex = Math.min(filteredClasses.size() - 1, selectedIndex + gridCols);
            return true;
        }
        if (keyCode == 263) { // LEFT
            selectedIndex = Math.max(0, selectedIndex - 1);
            return true;
        }
        if (keyCode == 262) { // RIGHT
            selectedIndex = Math.min(filteredClasses.size() - 1, selectedIndex + 1);
            return true;
        }
        if (keyCode == 257 && selectedIndex >= 0 && selectedIndex < filteredClasses.size()) { // ENTER
            Minecraft.getInstance().setScreen(new ClassDetailScreen(filteredClasses.get(selectedIndex), mode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= (int) (delta * 20);
        scrollOffset = Math.max(0, scrollOffset);
        return true;
    }

    @Override
    public void onClose() {
        if (mode == 1) {
            super.onClose();
        }
    }

    private void recalculateLayout() {
        int count = filteredClasses.size();
        if (count <= 6) {
            gridCols = 3;
            cardSize = 64;
        } else if (count <= 15) {
            gridCols = 5;
            cardSize = 48;
        } else if (count <= 24) {
            gridCols = 6;
            cardSize = 40;
        } else {
            gridCols = 6;
            cardSize = 40;
        }
        gridRows = (int) Math.ceil((double) count / gridCols);
        cardSpacing = 10;
        int gridWidth = gridCols * (cardSize + cardSpacing) - cardSpacing;
        gridStartX = (width - gridWidth) / 2;
        // Center cards vertically between title and lore panel
        int availableH = height - TITLE_HEIGHT - LORE_PANEL_HEIGHT - 10;
        int gridHeight = gridRows * (cardSize + cardSpacing + 14) - cardSpacing;
        gridStartY = TITLE_HEIGHT + Math.max(4, (availableH - gridHeight) / 2);
    }
}
