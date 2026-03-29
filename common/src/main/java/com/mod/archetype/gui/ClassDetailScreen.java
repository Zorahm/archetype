package com.mod.archetype.gui;

import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.core.PlayerClass.AttributeModifierEntry;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.network.ClassSelectPacket;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public class ClassDetailScreen extends Screen {

    private final PlayerClass playerClass;
    private final int mode;
    private float scrollOffset = 0;
    private float contentHeight = 0;
    private boolean showConfirmation = false;
    private float barAnimProgress = 0;
    private int selectBtnX, selectBtnY, selectBtnWidth, selectBtnHeight;

    // Hovered ability slot tracking (set during render, used after scissor)
    private int hoveredAbilitySlot = -1;
    private int tooltipMouseX = 0;
    private int tooltipMouseY = 0;

    // Layout constants
    private static final int PANEL_PADDING = 16;
    private static final int SECTION_GAP = 14;
    private static final int ITEM_GAP = 4;
    private static final int CARD_PADDING = 8;
    private static final int CARD_RADIUS_SIM = 2;
    private static final int SCROLLBAR_WIDTH = 3;

    public ClassDetailScreen(PlayerClass playerClass, int mode) {
        super(Component.translatable(playerClass.getNameKey()));
        this.playerClass = playerClass;
        this.mode = mode;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dark vignette background
        renderDimBackground(g);

        barAnimProgress = Math.min(1.0f, barAnimProgress + partialTick * 0.02f);

        int classColor = playerClass.getColor();
        int panelWidth = Math.min(420, width - 40);
        int panelX = (width - panelWidth) / 2;
        int viewTop = 16;
        int viewBottom = height - 16;
        int innerWidth = panelWidth - PANEL_PADDING * 2;

        // Main panel background
        renderPanel(g, panelX, viewTop, panelWidth, viewBottom - viewTop, classColor);

        // Content area with scissor
        int contentX = panelX + PANEL_PADDING;
        int scissorTop = viewTop + PANEL_PADDING;
        int scissorBottom = viewBottom - PANEL_PADDING;
        g.enableScissor(contentX, scissorTop, contentX + innerWidth, scissorBottom);

        int y = scissorTop - (int) scrollOffset;

        // Reset hovered ability slot each frame
        hoveredAbilitySlot = -1;

        // Header
        y = renderHeader(g, contentX, y, innerWidth, classColor);
        y += SECTION_GAP;

        // Separator line in class color
        renderColoredSeparator(g, contentX, y, innerWidth, classColor);
        y += SECTION_GAP;

        // Attributes
        if (!playerClass.getAttributes().isEmpty()) {
            y = renderSectionTitle(g, "gui.archetype.attributes", contentX, y, innerWidth, classColor);
            y += 6;
            y = renderAttributes(g, contentX, y, innerWidth);
            y += SECTION_GAP;
        }

        // Active abilities
        if (!playerClass.getActiveAbilities().isEmpty()) {
            y = renderSectionTitle(g, "gui.archetype.abilities", contentX, y, innerWidth, classColor);
            y += 6;
            y = renderActiveAbilities(g, contentX, y, innerWidth, classColor, mouseX, mouseY);
            y += SECTION_GAP;
        }

        // Passive abilities
        if (!playerClass.getPassiveAbilities().isEmpty()) {
            y = renderSectionTitle(g, "gui.archetype.passives", contentX, y, innerWidth, classColor);
            y += 6;
            y = renderPassives(g, contentX, y, innerWidth);
            y += SECTION_GAP;
        }

        // Resource
        if (playerClass.getResource() != null) {
            y = renderSectionTitle(g, "gui.archetype.resource", contentX, y, innerWidth, classColor);
            y += 6;
            y = renderResource(g, contentX, y, innerWidth);
            y += SECTION_GAP;
        }

        // Select button
        y = renderSelectButton(g, contentX, y, innerWidth, mouseX, mouseY, classColor);

        contentHeight = y + scrollOffset - scissorTop + PANEL_PADDING;

        g.disableScissor();

        // Ability slot tooltip (rendered after disableScissor)
        if (hoveredAbilitySlot >= 0 && hoveredAbilitySlot < playerClass.getActiveAbilities().size()) {
            renderAbilityTooltip(g, playerClass.getActiveAbilities().get(hoveredAbilitySlot),
                    tooltipMouseX, tooltipMouseY, innerWidth, classColor);
        }

        // Scrollbar
        float viewHeight = scissorBottom - scissorTop;
        if (contentHeight > viewHeight) {
            renderScrollbar(g, panelX + panelWidth - SCROLLBAR_WIDTH - 4, scissorTop, viewHeight, classColor);
        }

        // Back button (rendered outside scissor)
        renderBackButton(g, panelX + PANEL_PADDING, viewBottom - 4, mouseX, mouseY);

        // Render widgets (vanilla buttons)
        super.render(g, mouseX, mouseY, partialTick);

        if (showConfirmation) {
            renderConfirmation(g, mouseX, mouseY, classColor);
        }
    }

    private void renderDimBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xCC000000);
        // Vignette gradients
        g.fillGradient(0, 0, width / 4, height, 0x88000000, 0x00000000);
        g.fillGradient(width * 3 / 4, 0, width, height, 0x00000000, 0x88000000);
        g.fillGradient(0, 0, width, height / 4, 0x88000000, 0x00000000);
        g.fillGradient(0, height * 3 / 4, width, height, 0x00000000, 0x88000000);
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, int classColor) {
        // Main panel fill
        g.fill(x, y, x + w, y + h, 0xE6111122);

        // Thin class-colored border
        int borderColor = 0xCC000000 | classColor;
        // Top
        g.fill(x, y, x + w, y + 1, borderColor);
        // Bottom
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        // Left
        g.fill(x, y, x + 1, y + h, borderColor);
        // Right
        g.fill(x + w - 1, y, x + w, y + h, borderColor);

        // Top accent line (brighter, 2px)
        int accentColor = 0xFF000000 | classColor;
        g.fill(x + 1, y, x + w - 1, y + 2, accentColor);
    }

    private void renderColoredSeparator(GuiGraphics g, int x, int y, int width, int classColor) {
        // Gradient-like separator: fade from center
        int mid = x + width / 2;
        int halfW = width / 2;
        // Center section — brighter
        int coreHalf = halfW / 3;
        g.fill(mid - coreHalf, y, mid + coreHalf, y + 1, 0x60000000 | classColor);
        // Sides — dimmer
        g.fill(x, y, mid - coreHalf, y + 1, 0x20000000 | classColor);
        g.fill(mid + coreHalf, y, x + width, y + 1, 0x20000000 | classColor);
    }

    private int renderSectionTitle(GuiGraphics g, String titleKey, int x, int y, int width, int classColor) {
        Component title = Component.translatable(titleKey).withStyle(Style.EMPTY.withColor(0xA0A0A0));
        g.drawString(font, title, x, y, 0xA0A0A0, false);
        int textWidth = font.width(title);
        // Line after text
        g.fill(x + textWidth + 6, y + font.lineHeight / 2, x + width, y + font.lineHeight / 2 + 1, 0x20FFFFFF);
        return y + font.lineHeight + 2;
    }

    private int renderHeader(GuiGraphics g, int x, int y, int width, int classColor) {
        // Class name (scaled, centered)
        var pose = g.pose();
        pose.pushPose();
        float scale = 1.8f;
        Component name = Component.translatable(playerClass.getNameKey());
        int nameWidth = (int) (font.width(name) * scale);
        float nameX = x + (width - nameWidth) / 2f;
        pose.translate(nameX, y, 0);
        pose.scale(scale, scale, 1);
        g.drawString(font, name, 0, 0, 0xFF000000 | classColor, false);
        pose.popPose();
        y += (int) (font.lineHeight * scale) + 6;

        // Lore — with word wrapping
        if (!playerClass.getLoreKeys().isEmpty()) {
            Component lore = Component.translatable(playerClass.getLoreKeys().get(0))
                    .withStyle(s -> s.withItalic(true).withColor(0x808080));
            List<FormattedCharSequence> loreLines = font.split(lore, width);
            for (FormattedCharSequence line : loreLines) {
                int lineWidth = font.width(line);
                g.drawString(font, line, x + (width - lineWidth) / 2, y, 0x808080, false);
                y += font.lineHeight + 1;
            }
        }
        return y;
    }

    private int renderAttributes(GuiGraphics g, int x, int y, int contentWidth) {
        int barWidth = Math.min(120, contentWidth / 3);
        for (AttributeModifierEntry attr : playerClass.getAttributes()) {
            String attrName = Component.translatable("gui.archetype.attr." + attr.attribute().getPath().replace("generic.", "")).getString();
            double baseValue = getBaseValue(attr.attribute().toString());
            double value = baseValue + attr.value() * barAnimProgress;
            ClassScreenRenderer.renderAttributeBar(g, font, attrName, value, baseValue, x, y, barWidth);
            y += 14;
        }
        return y;
    }

    private int renderActiveAbilities(GuiGraphics g, int x, int y, int contentWidth, int classColor, int mouseX, int mouseY) {
        String[] slotKeys = {"R", "V", "G"};
        List<ActiveAbilityEntry> abilities = playerClass.getActiveAbilities();

        int slotSize = 32;
        int slotSpacing = 6;
        int totalRowWidth = abilities.size() * slotSize + (abilities.size() - 1) * slotSpacing;

        for (int i = 0; i < abilities.size(); i++) {
            ActiveAbilityEntry ability = abilities.get(i);
            int slotIndex = switch (ability.slot()) {
                case "ability_1" -> 0;
                case "ability_2" -> 1;
                case "ability_3" -> 2;
                default -> 0;
            };
            String key = slotIndex < slotKeys.length ? slotKeys[slotIndex] : "?";

            int slotX = x + i * (slotSize + slotSpacing);
            int slotY = y;

            // Slot background
            g.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x28FFFFFF);
            // 1px border using classColor
            int borderColor = 0x50000000 | classColor;
            g.fill(slotX, slotY, slotX + slotSize, slotY + 1, borderColor);
            g.fill(slotX, slotY + slotSize - 1, slotX + slotSize, slotY + slotSize, borderColor);
            g.fill(slotX, slotY, slotX + 1, slotY + slotSize, borderColor);
            g.fill(slotX + slotSize - 1, slotY, slotX + slotSize, slotY + slotSize, borderColor);

            // Key badge at top-left
            String badge = "[" + key + "]";
            int badgeBg = 0xC0000000 | classColor;
            g.fill(slotX + 1, slotY + 1, slotX + 1 + font.width(badge) + 2, slotY + 1 + font.lineHeight, badgeBg);
            g.drawString(font, badge, slotX + 2, slotY + 1, 0xFFFFFF, false);

            // Ability name (truncated to fit slot width)
            Component aName = Component.translatable(ability.nameKey());
            String nameStr = aName.getString();
            int maxNameW = slotSize - 4;
            // Truncate if needed
            while (font.width(nameStr) > maxNameW && nameStr.length() > 1) {
                nameStr = nameStr.substring(0, nameStr.length() - 1);
            }
            if (!nameStr.equals(aName.getString())) {
                nameStr = nameStr.substring(0, Math.max(0, nameStr.length() - 2)) + "..";
            }
            g.drawString(font, nameStr, slotX + 2, slotY + slotSize - font.lineHeight - 2, 0xDDDDDD, false);

            // Hover detection
            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                hoveredAbilitySlot = i;
                tooltipMouseX = mouseX;
                tooltipMouseY = mouseY;
            }
        }

        y += slotSize + 4;

        // Extra sections rendered after all ability slots
        for (ActiveAbilityEntry ability : abilities) {
            for (PlayerClass.ExtraAbilitySection section : playerClass.getExtraAbilitySections()) {
                if (section.parentSlot().equals(ability.slot())) {
                    y = renderExtraAbilitySection(g, x, y, contentWidth, section, classColor);
                }
            }
        }

        return y;
    }

    private void renderAbilityTooltip(GuiGraphics g, ActiveAbilityEntry ability, int mouseX, int mouseY,
                                       int contentWidth, int classColor) {
        String[] slotKeys = {"R", "V", "G"};
        int slotIndex = switch (ability.slot()) {
            case "ability_1" -> 0;
            case "ability_2" -> 1;
            case "ability_3" -> 2;
            default -> 0;
        };
        String key = slotIndex < slotKeys.length ? slotKeys[slotIndex] : "?";

        Component aName = Component.translatable(ability.nameKey());
        Component desc = Component.translatable(ability.descriptionKey());

        int tipW = Math.min(200, contentWidth);
        List<FormattedCharSequence> descLines = font.split(desc, tipW - 12);
        int tipH = font.lineHeight + 4 + descLines.size() * (font.lineHeight + 1) + 8;

        int tipX = mouseX + 10;
        int tipY = mouseY - tipH / 2;

        // Keep on screen
        if (tipX + tipW > width - 4) tipX = mouseX - tipW - 4;
        if (tipY + tipH > height - 4) tipY = height - tipH - 4;
        if (tipY < 4) tipY = 4;

        // Background
        g.fill(tipX, tipY, tipX + tipW, tipY + tipH, 0xFF0E0E18);
        // Border
        int bc = 0xCC000000 | classColor;
        g.fill(tipX, tipY, tipX + tipW, tipY + 1, bc);
        g.fill(tipX, tipY + tipH - 1, tipX + tipW, tipY + tipH, bc);
        g.fill(tipX, tipY, tipX + 1, tipY + tipH, bc);
        g.fill(tipX + tipW - 1, tipY, tipX + tipW, tipY + tipH, bc);

        // Key badge + name
        String badge = "[" + key + "] ";
        int badgeW = font.width(badge);
        g.drawString(font, badge, tipX + 6, tipY + 4, 0xFF000000 | classColor, false);
        g.drawString(font, aName, tipX + 6 + badgeW, tipY + 4, 0xFFFFFF, false);

        int ty = tipY + 4 + font.lineHeight + 2;
        for (FormattedCharSequence line : descLines) {
            g.drawString(font, line, tipX + 6, ty, 0x999999, false);
            ty += font.lineHeight + 1;
        }
    }

    private int renderExtraAbilitySection(GuiGraphics g, int x, int y, int contentWidth,
                                           PlayerClass.ExtraAbilitySection section, int classColor) {
        boolean locked = section.unlockLevel() > 0;

        // Section title centered with decorative lines
        Component sectionName = Component.translatable(section.nameKey());
        int nameWidth = font.width(sectionName);
        int centerX = x + contentWidth / 2;
        int textStartX = centerX - nameWidth / 2 - 6;
        int textEndX = centerX + nameWidth / 2 + 6;

        g.fill(x + 8, y + font.lineHeight / 2, textStartX, y + font.lineHeight / 2 + 1, 0x30000000 | classColor);
        g.drawString(font, sectionName, centerX - nameWidth / 2, y, 0xFF000000 | classColor, false);
        g.fill(textEndX, y + font.lineHeight / 2, x + contentWidth - 8, y + font.lineHeight / 2 + 1, 0x30000000 | classColor);
        y += font.lineHeight + 6;

        // In detail screen (pre-select), always show entries, but note unlock level if any
        if (locked) {
            Component lockNote = Component.translatable("gui.archetype.locked", section.unlockLevel());
            g.drawString(font, lockNote, x + 8, y, 0x888888, false);
            y += font.lineHeight + 4;
        }

        for (PlayerClass.ExtraAbilityEntry entry : section.entries()) {
            Component entryName = Component.translatable(entry.nameKey());
            Component entryDesc = Component.translatable(entry.descriptionKey());

            List<FormattedCharSequence> dLines = font.split(entryDesc, contentWidth - 28);
            int totalH = font.lineHeight + dLines.size() * (font.lineHeight + 1) + 4;
            g.fill(x + 4, y - 2, x + contentWidth - 12, y + totalH, 0x10000000 | classColor);
            g.fill(x + 4, y - 2, x + 6, y + totalH, 0x40000000 | classColor);

            g.drawString(font, entryName, x + 10, y, 0xDDDDDD, false);
            y += font.lineHeight + 1;

            for (FormattedCharSequence dLine : dLines) {
                g.drawString(font, dLine, x + 10, y, 0x888888, false);
                y += font.lineHeight + 1;
            }
            y += 4;
        }
        return y;
    }

    private int renderPassives(GuiGraphics g, int x, int y, int contentWidth) {
        var sorted = playerClass.getPassiveAbilities().stream()
                .filter(p -> !p.hidden())
                .sorted((a, b) -> Boolean.compare(b.positive(), a.positive()))
                .toList();
        for (PassiveAbilityEntry passive : sorted) {
            boolean positive = passive.positive();
            int color = positive ? 0xFF44CC44 : 0xFFCC4444;
            int bgColor = positive ? 0x18008800 : 0x18880000;
            String marker = positive ? "\u2714" : "\u2718";

            // Pre-calculate description height
            Component pDesc = Component.translatable(passive.descriptionKey());
            List<FormattedCharSequence> descLines = font.split(pDesc, contentWidth - 24);
            int descHeight = descLines.size() * (font.lineHeight + 1);

            // Card background
            int cardTop = y - CARD_PADDING / 2;
            int cardBottom = y + 12 + descHeight + CARD_PADDING / 2;
            g.fill(x, cardTop, x + contentWidth - 8, cardBottom, bgColor);
            // Left accent stripe
            g.fill(x, cardTop, x + 2, cardBottom, 0x80000000 | (color & 0x00FFFFFF));

            g.drawString(font, marker, x + 6, y, color, false);
            Component pName = Component.translatable(passive.nameKey());
            g.drawString(font, pName, x + 18, y, color, false);
            y += 12;

            // Description — WORD WRAPPED
            for (FormattedCharSequence line : descLines) {
                g.drawString(font, line, x + 18, y, 0x999999, false);
                y += font.lineHeight + 1;
            }
            y += ITEM_GAP + 2;
        }
        return y;
    }

    private int renderResource(GuiGraphics g, int x, int y, int contentWidth) {
        var res = playerClass.getResource();
        Component resName = Component.translatable(res.typeKey());
        g.drawString(font, resName, x, y, 0xFF000000 | res.color(), false);
        y += 14;
        // Bar full inner width
        int barW = contentWidth;
        ClassScreenRenderer.renderProgressBar(g, x, y, barW, 8, 1.0f, res.color());
        g.drawString(font, res.maxValue() + "/" + res.maxValue(), x + barW + 6, y, 0xCCCCCC, false);
        y += 14;
        return y;
    }

    private int renderSelectButton(GuiGraphics g, int x, int y, int contentWidth, int mouseX, int mouseY, int classColor) {
        int btnWidth = contentWidth;
        int btnHeight = 32;
        int btnX = x;
        int btnY = y + 8;

        this.selectBtnX = btnX;
        this.selectBtnY = btnY;
        this.selectBtnWidth = btnWidth;
        this.selectBtnHeight = btnHeight;

        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        int baseColor = classColor;
        int bgColor = hovered ? (0xE0000000 | baseColor) : (0x60000000 | baseColor);
        g.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, bgColor);

        // Border
        int borderAlpha = hovered ? 0xFF : 0xCC;
        int borderColor = (borderAlpha << 24) | baseColor;
        g.fill(btnX, btnY, btnX + btnWidth, btnY + 1, borderColor);
        g.fill(btnX, btnY + btnHeight - 1, btnX + btnWidth, btnY + btnHeight, borderColor);
        g.fill(btnX, btnY, btnX + 1, btnY + btnHeight, borderColor);
        g.fill(btnX + btnWidth - 1, btnY, btnX + btnWidth, btnY + btnHeight, borderColor);

        Component text = Component.translatable("gui.archetype.select_class");
        g.drawCenteredString(font, text, btnX + btnWidth / 2, btnY + (btnHeight - font.lineHeight) / 2, 0xFFFFFF);

        return btnY + btnHeight;
    }

    private void renderScrollbar(GuiGraphics g, int x, int top, float viewHeight, int classColor) {
        float maxScroll = Math.max(1, contentHeight - viewHeight);
        float thumbRatio = viewHeight / contentHeight;
        int thumbHeight = Math.max(10, (int) (viewHeight * thumbRatio));
        int thumbY = top + (int) ((viewHeight - thumbHeight) * (scrollOffset / maxScroll));

        // Track
        g.fill(x, (int) top, x + SCROLLBAR_WIDTH, (int) (top + viewHeight), 0x15FFFFFF);
        // Thumb
        g.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0x80000000 | classColor);
    }

    private void renderBackButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        Component back = Component.translatable("gui.archetype.back");
        int bw = font.width(back) + 12;
        int bh = 16;
        int bx = x;
        int by = y - bh;
        boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;

        g.fill(bx, by, bx + bw, by + bh, hovered ? 0x40FFFFFF : 0x20FFFFFF);
        g.drawString(font, back, bx + 6, by + (bh - font.lineHeight) / 2, hovered ? 0xFFFFFF : 0xAAAAAA, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showConfirmation) {
            int yesX = width / 2 - 80;
            int yesY = height / 2 + 10;
            if (mouseX >= yesX && mouseX <= yesX + 60 && mouseY >= yesY && mouseY <= yesY + 20) {
                NetworkHandler.INSTANCE.sendToServer(new ClassSelectPacket(playerClass.getId(), mode == 1));
                onClose();
                return true;
            }
            int noX = width / 2 + 20;
            if (mouseX >= noX && mouseX <= noX + 60 && mouseY >= yesY && mouseY <= yesY + 20) {
                showConfirmation = false;
                return true;
            }
            return true;
        }

        // Back button check
        int panelWidth = Math.min(420, width - 40);
        int panelX = (width - panelWidth) / 2;
        int viewBottom = height - 16;
        int bx = panelX + PANEL_PADDING;
        Component back = Component.translatable("gui.archetype.back");
        int bw = font.width(back) + 12;
        int bh = 16;
        int by = viewBottom - 4 - bh;
        if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh && button == 0) {
            Minecraft.getInstance().setScreen(new ClassSelectionScreen(mode));
            return true;
        }

        // Select button click detection — only the button area
        if (mouseX >= selectBtnX && mouseX <= selectBtnX + selectBtnWidth
                && mouseY >= selectBtnY && mouseY <= selectBtnY + selectBtnHeight && button == 0) {
            if (mode == 0) {
                NetworkHandler.INSTANCE.sendToServer(new ClassSelectPacket(playerClass.getId(), false));
                onClose();
                return true;
            } else {
                showConfirmation = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= (float) (delta * 20);
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - (height - 64)));
        return true;
    }

    private void renderConfirmation(GuiGraphics g, int mouseX, int mouseY, int classColor) {
        // Full-screen darken
        g.fill(0, 0, width, height, 0xE0000000);

        // Dialog box
        int dw = 240;
        int dh = 80;
        int dx = (width - dw) / 2;
        int dy = (height - dh) / 2;
        g.fill(dx, dy, dx + dw, dy + dh, 0xFF0C0C14);
        // Border
        int bc = 0xCC000000 | classColor;
        g.fill(dx, dy, dx + dw, dy + 1, bc);
        g.fill(dx, dy + dh - 1, dx + dw, dy + dh, bc);
        g.fill(dx, dy, dx + 1, dy + dh, bc);
        g.fill(dx + dw - 1, dy, dx + dw, dy + dh, bc);

        Component message = Component.translatable("gui.archetype.confirm_rebirth");
        g.drawCenteredString(font, message, width / 2, dy + 16, 0xFFFFFF);

        // Yes button
        int yesX = width / 2 - 80;
        int yesY = height / 2 + 10;
        boolean yesHover = mouseX >= yesX && mouseX <= yesX + 60 && mouseY >= yesY && mouseY <= yesY + 20;
        g.fill(yesX, yesY, yesX + 60, yesY + 20, yesHover ? 0xFF3A5A3A : 0xFF1A1A1A);
        g.fill(yesX, yesY, yesX + 60, yesY + 1, 0xCC44CC44);
        g.drawCenteredString(font, Component.translatable("gui.yes"), yesX + 30, yesY + 6, 0x44CC44);

        // No button
        int noX = width / 2 + 20;
        boolean noHover = mouseX >= noX && mouseX <= noX + 60 && mouseY >= yesY && mouseY <= yesY + 20;
        g.fill(noX, yesY, noX + 60, yesY + 20, noHover ? 0xFF5A3A3A : 0xFF1A1A1A);
        g.fill(noX, yesY, noX + 60, yesY + 1, 0xCCCC4444);
        g.drawCenteredString(font, Component.translatable("gui.no"), noX + 30, yesY + 6, 0xCC4444);
    }

    private double getBaseValue(String attributeId) {
        return switch (attributeId) {
            case "minecraft:generic.max_health" -> 20.0;
            case "minecraft:generic.attack_damage" -> 1.0;
            case "minecraft:generic.movement_speed" -> 0.1;
            case "minecraft:generic.armor" -> 0.0;
            case "minecraft:generic.armor_toughness" -> 0.0;
            case "minecraft:generic.attack_speed" -> 4.0;
            case "minecraft:generic.knockback_resistance" -> 0.0;
            default -> 0.0;
        };
    }
}
