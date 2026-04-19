package com.mod.archetype.gui;

import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ClassSelectionScreen extends Screen {

    private final int mode; // 0=first, 1=rebirth
    private List<PlayerClass> allClasses;
    private List<PlayerClass> filteredClasses;
    private int hoveredIndex = -1;
    private int selectedIndex = -1;
    private float[] cardScales;
    private int scrollOffset = 0;
    @Nullable
    private PlayerClass hoveredClass;
    @Nullable
    private PlayerClass lastHoveredClass;
    private float hoverPanelAlpha = 0f;

    private int gridCols, gridRows, cardW, cardH, gridStartX, gridStartY, cardSpacing;

    // Layout constants
    private static final int INFO_PANEL_W = 200;
    private static final int INFO_PANEL_MARGIN = 16;
    private static final int TITLE_HEIGHT = 40;
    private static final int BOTTOM_PAD = 50;

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

    private int getGridAreaWidth() {
        return width - INFO_PANEL_W - INFO_PANEL_MARGIN * 2 - 10;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. Background (Полупрозрачный с легкой виньеткой)
        renderDimBackground(graphics);

        // 2. Title
        renderTitle(graphics);

        // 3. Scissor for card grid
        int gridAreaW = getGridAreaWidth();
        graphics.enableScissor(0, TITLE_HEIGHT, gridAreaW, height - BOTTOM_PAD);

        // 4. Cards — update hover state and lerp scales
        hoveredIndex = -1;
        hoveredClass = null;
        for (int i = 0; i < filteredClasses.size(); i++) {
            int row = i / gridCols;
            int col = i % gridCols;
            int x = gridStartX + col * (cardW + cardSpacing);
            int y = gridStartY + row * (cardH + cardSpacing + 16) - scrollOffset; // Чуть больше отступ по вертикали

            if (y + cardH < TITLE_HEIGHT || y > height - BOTTOM_PAD) continue;

            PlayerClass cls = filteredClasses.get(i);
            boolean hovered = mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH
                    && mouseX < gridAreaW;
            if (hovered) {
                hoveredIndex = i;
                hoveredClass = cls;
            }

            float targetScale = hovered ? 1.08f : 1.0f;
            if (i < cardScales.length) {
                cardScales[i] = Mth.lerp(0.25f, cardScales[i], targetScale);
            }

            boolean isSelected = i == selectedIndex;
            renderBannerCard(graphics, cls, x, y, i < cardScales.length ? cardScales[i] : 1.0f, hovered, isSelected);
        }

        graphics.disableScissor();

        // 5. Lerp hoverPanelAlpha
        float alphaTarget = hoveredClass != null ? 1.0f : 0.0f;
        hoverPanelAlpha = Mth.lerp(0.18f, hoverPanelAlpha, alphaTarget);
        if (hoveredClass != null) {
            lastHoveredClass = hoveredClass;
        }

        // 6. Info panel
        if (hoverPanelAlpha > 0.01f && lastHoveredClass != null) {
            renderInfoPanel(graphics, lastHoveredClass, mouseX, mouseY);
        }

        // 7. Random button
        renderRandomButton(graphics, mouseX, mouseY);

        // 8. Super
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDimBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    private void renderTitle(GuiGraphics g) {
        int gridAreaW = getGridAreaWidth();
        int titleCenterX = gridAreaW / 2;
        int titleY = (TITLE_HEIGHT - font.lineHeight) / 2 - 2;

        var pose = g.pose();
        pose.pushMatrix();
        float scale = 1.5f;
        int titleW = (int) (font.width(title) * scale);
        float titleX = titleCenterX - titleW / 2f;
        pose.translate(titleX, titleY);
        pose.scale(scale, scale);
        g.drawString(font, title, 0, 0, 0xFFFFFFFF, true);
        pose.popMatrix();

        // Красивый градиентный разделитель под заголовком
        int lineY = TITLE_HEIGHT - 2;
        int lineWidth = gridAreaW / 2;
        g.fill(titleCenterX - lineWidth / 2, lineY, titleCenterX + lineWidth / 2, lineY + 1, 0x30FFFFFF);
        g.fill(titleCenterX - lineWidth / 6, lineY, titleCenterX + lineWidth / 6, lineY + 1, 0x50FFFFFF);
    }

    private void renderBannerCard(GuiGraphics g, PlayerClass cls, int x, int y, float scale,
                                  boolean hovered, boolean isSelected) {
        int classColor = cls.getColor();
        var pose = g.pose();
        boolean transformed = scale != 1.0f;

        if (transformed) {
            float cx = x + cardW / 2f;
            float cy = y + cardH / 2f;
            pose.pushMatrix();
            pose.translate(cx, cy);
            pose.scale(scale, scale);
            pose.translate(-cx, -cy);
        }

        int bgAlpha = hovered ? 0xDD : 0xAA;
        g.fill(x, y, x + cardW, y + cardH, (bgAlpha << 24) | 0x111118);

        Identifier classIcon = cls.getIcon();
        int artSize = Math.min((int)(cardW * 1.6f), cardH - 14);
        int artX = x + (cardW - artSize) / 2;
        int artY = y + cardH - artSize - 10;
        g.blit(RenderPipelines.GUI_TEXTURED, classIcon, artX, artY, 0f, 0f, artSize, artSize, 1024, 1024, 1024, 1024);

        int gradientTop = y + cardH / 2;
        g.fillGradient(x, gradientTop, x + cardW, y + cardH, 0x00000000, 0xDD000000 | classColor);

        int borderColor;
        if (isSelected) {
            borderColor = 0xFFFFFFFF; // Яркая белая рамка при выборе с клавиатуры
        } else {
            int borderAlpha = hovered ? 0xFF : 0x40;
            borderColor = (borderAlpha << 24) | classColor;
        }

        g.fill(x, y, x + cardW, y + 1, borderColor);
        g.fill(x, y + cardH - 1, x + cardW, y + cardH, borderColor);
        g.fill(x, y, x + 1, y + cardH, borderColor);
        g.fill(x + cardW - 1, y, x + cardW, y + cardH, borderColor);

        // Имя класса (с центрированием и тенью)
        Component name = Component.translatable(cls.getNameKey());
        int textY = y + cardH - font.lineHeight - 6;
        g.drawCenteredString(font, name, x + cardW / 2, textY, 0xFFFFFFFF);

        if (transformed) {
            pose.popMatrix();
        }
    }

    private void renderInfoPanel(GuiGraphics g, PlayerClass cls, int mouseX, int mouseY) {
        int panelX = width - INFO_PANEL_W - INFO_PANEL_MARGIN;
        int panelY = TITLE_HEIGHT;
        int panelH = height - TITLE_HEIGHT - BOTTOM_PAD + 10;
        int classColor = cls.getColor();

        // Плавно меняющаяся альфа для всей панели
        int alphaValue = (int) (hoverPanelAlpha * 255);
        if (alphaValue < 5) return; // Оптимизация

        // Фоновая подложка панели
        int bgAlpha = (int) (hoverPanelAlpha * 240);
        g.fill(panelX, panelY, panelX + INFO_PANEL_W, panelY + panelH, (bgAlpha << 24) | 0x0A0A10);

        // Рамка панели цвета класса
        int borderAlpha = (int) (hoverPanelAlpha * 150);
        int borderColor = (borderAlpha << 24) | classColor;
        g.fill(panelX, panelY, panelX + INFO_PANEL_W, panelY + 1, borderColor);
        g.fill(panelX, panelY + panelH - 1, panelX + INFO_PANEL_W, panelY + panelH, borderColor);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, borderColor);
        g.fill(panelX + INFO_PANEL_W - 1, panelY, panelX + INFO_PANEL_W, panelY + panelH, borderColor);

        int innerX = panelX + 12;
        int innerW = INFO_PANEL_W - 24;
        int ty = panelY + 14;

        // ВАЖНО: Применяем альфу к цвету текста, чтобы текст не "прыгал", а плавно появлялся
        int textAlphaMask = alphaValue << 24;

        // Имя класса (Крупно)
        Component name = Component.translatable(cls.getNameKey());
        var pose = g.pose();
        pose.pushMatrix();
        float scale = 1.3f;
        pose.translate(innerX, ty);
        pose.scale(scale, scale);
        g.drawString(font, name, 0, 0, textAlphaMask | (classColor & 0x00FFFFFF), true);
        pose.popMatrix();
        ty += (int) (font.lineHeight * scale) + 6;

        // Разделитель
        int sepColorCenter = (int)(hoverPanelAlpha * 100) << 24 | (classColor & 0x00FFFFFF);
        int sepColorEdge = (int)(hoverPanelAlpha * 20) << 24 | (classColor & 0x00FFFFFF);
        g.fill(innerX, ty, innerX + innerW / 2, ty + 1, sepColorCenter);
        g.fill(innerX + innerW / 2, ty, innerX + innerW, ty + 1, sepColorEdge);
        ty += 8;

        // Лор (описание)
        if (!cls.getLoreKeys().isEmpty()) {
            Component lore = Component.translatable(cls.getLoreKeys().get(0))
                    .withStyle(s -> s.withItalic(true));
            List<FormattedCharSequence> loreLines = font.split(lore, innerW);
            for (int i = 0; i < Math.min(loreLines.size(), 6); i++) {
                g.drawString(font, loreLines.get(i), innerX, ty, textAlphaMask | 0x999999, false);
                ty += font.lineHeight + 2;
            }
            ty += 8;
        }

        // Компактные индикаторы способностей в одну строку
        int abilityCount = cls.getActiveAbilities().size();
        int positiveCount = (int) cls.getPassiveAbilities().stream().filter(p -> !p.hidden() && p.positive()).count();
        int negativeCount = (int) cls.getPassiveAbilities().stream().filter(p -> !p.hidden() && !p.positive()).count();

        int rightEdge = panelX + INFO_PANEL_W - 12;
        int gap = 10;
        int curX = rightEdge;

        // Негативные пассивки (красный)
        if (negativeCount > 0) {
            String negStr = "\u25BC" + negativeCount;
            curX -= font.width(negStr);
            g.drawString(font, negStr, curX, ty, textAlphaMask | 0xFF6666, false);
            curX -= gap;
        }

        // Позитивные пассивки (зелёный)
        if (positiveCount > 0) {
            String posStr = "\u25B2" + positiveCount;
            curX -= font.width(posStr);
            g.drawString(font, posStr, curX, ty, textAlphaMask | 0x66FF66, false);
            curX -= gap;
        }

        // Активные навыки (голубой)
        if (abilityCount > 0) {
            String abStr = "\u26A1" + abilityCount;
            curX -= font.width(abStr);
            g.drawString(font, abStr, curX, ty, textAlphaMask | 0x88CCFF, false);
        }

        ty += font.lineHeight + 4;

        // Подсказка в самом низу панели
        Component hint = Component.translatable("gui.archetype.click_to_view");
        int hintW = font.width(hint);
        int hintY = panelY + panelH - font.lineHeight - 12;

        // Рисуем легкий фон под подсказкой
        g.fill(panelX + 4, hintY - 4, panelX + INFO_PANEL_W - 4, hintY + font.lineHeight + 4, textAlphaMask | 0x1AFFFFFF);
        g.drawString(font, hint, panelX + (INFO_PANEL_W - hintW) / 2, hintY, textAlphaMask | 0xAAAAAA, false);
    }

    private void renderRandomButton(GuiGraphics g, int mouseX, int mouseY) {
        int gridAreaW = getGridAreaWidth();
        Component text = Component.literal("\u2684 ").append(Component.translatable("gui.archetype.random"));
        int bw = font.width(text) + 24; // Кнопка стала чуть шире
        int bh = 22;
        int bx = gridAreaW / 2 - bw / 2;
        int by = height - BOTTOM_PAD + (BOTTOM_PAD - bh) / 2;
        boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;

        // Эффект стеклянной кнопки
        g.fill(bx, by, bx + bw, by + bh, hovered ? 0x60FFFFFF : 0x20FFFFFF); // Фон
        g.fill(bx, by, bx + bw, by + 1, hovered ? 0x50FFFFFF : 0x25FFFFFF); // Верхний блик
        g.fill(bx, by + bh - 1, bx + bw, by + bh, 0x15FFFFFF); // Нижняя тень

        // Боковые рамки
        g.fill(bx, by, bx + 1, by + bh, 0x30FFFFFF);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, 0x30FFFFFF);

        g.drawCenteredString(font, text, bx + bw / 2, by + (bh - font.lineHeight) / 2 + 1, hovered ? 0xFFFFFFFF : 0xFFDDDDDD);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            int gridAreaW = getGridAreaWidth();
            Component text = Component.literal("\u2684 ").append(Component.translatable("gui.archetype.random"));
            int bw = font.width(text) + 24;
            int bh = 22;
            int bx = gridAreaW / 2 - bw / 2;
            int by = height - BOTTOM_PAD + (BOTTOM_PAD - bh) / 2;

            if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                if (!filteredClasses.isEmpty()) {
                    PlayerClass randomClass = filteredClasses.get(new Random().nextInt(filteredClasses.size()));
                    Minecraft.getInstance().setScreen(new ClassDetailScreen(randomClass, mode));
                }
                return true;
            }

            if (hoveredIndex >= 0 && hoveredIndex < filteredClasses.size()) {
                Minecraft.getInstance().setScreen(new ClassDetailScreen(filteredClasses.get(hoveredIndex), mode));
                return true;
            }
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 256 && mode == 0) {
            return true;
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
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int availableH = height - TITLE_HEIGHT - BOTTOM_PAD - 10;
        int gridHeight = gridRows * (cardH + cardSpacing + 16) - cardSpacing;
        int maxScroll = Math.max(0, gridHeight - availableH);
        if (maxScroll == 0) return false;
        scrollOffset -= (int) (deltaY * 20);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
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
        if (count == 0) {
            gridCols = 1;
            gridRows = 0;
            cardW = 64;
            cardH = 96;
            gridStartX = 0;
            gridStartY = TITLE_HEIGHT + 10;
            cardSpacing = 12;
            return;
        }

        int gridAreaW = getGridAreaWidth();
        cardSpacing = 14; // Сделал отступы между карточками чуть больше для "воздуха"

        gridCols = Math.min(count, 4);
        while (gridCols > 1) {
            int computedW = (gridAreaW - (gridCols - 1) * cardSpacing) / gridCols;
            if (computedW >= 60) break;
            gridCols--;
        }

        cardW = (gridAreaW - (gridCols - 1) * cardSpacing) / gridCols;
        cardW = Mth.clamp(cardW, 60, 110);
        cardH = cardW * 3 / 2;

        gridRows = (int) Math.ceil((double) count / gridCols);

        int gridWidth = gridCols * cardW + (gridCols - 1) * cardSpacing;
        gridStartX = (gridAreaW - gridWidth) / 2;

        int availableH = height - TITLE_HEIGHT - BOTTOM_PAD - 10;
        int gridHeight = gridRows * (cardH + cardSpacing + 16) - cardSpacing;
        gridStartY = TITLE_HEIGHT + 10 + Math.max(0, (availableH - gridHeight) / 2);
    }
}