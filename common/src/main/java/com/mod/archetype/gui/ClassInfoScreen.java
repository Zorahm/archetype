package com.mod.archetype.gui;

import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import com.mod.archetype.core.PlayerClass.AttributeModifierEntry;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.google.gson.JsonObject;
import org.joml.Quaternionf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class ClassInfoScreen extends Screen {

    private PlayerClass playerClass;
    private float scrollOffset = 0;
    private float contentHeight = 0;
    private float leftScrollOffset = 0;
    private float leftContentHeight = 0;
    private int selectedTab = 0;

    private static final int PANEL_PADDING = 14;
    private static final int SECTION_GAP = 12;
    private static final int CARD_PAD = 6;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int DIVIDER_WIDTH = 1;
    private int levelBarX, levelBarY, levelBarW;
    private int cachedLeftX, cachedDividerX, cachedContentTop, cachedContentBottom, cachedLeftScrollTop;
    private int cachedRightX, cachedRightWidth, cachedTabHeaderHeight;

    // Animation state
    private long openTime;
    private float smoothScrollOffset = 0;
    private float smoothLeftScrollOffset = 0;
    private float smoothLevelProgress = -1f;
    private float tabTransition = 1f;

    // Smoothed model rotation
    private float smoothModelDx = 0;
    private float smoothModelDy = 0;

    public ClassInfoScreen() {
        super(Component.translatable("gui.archetype.class_info"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        ClientClassData data = ClientClassData.getInstance();
        if (data.hasClass() && data.getClassId() != null) {
            playerClass = ClassRegistry.getInstance().get(data.getClassId()).orElse(null);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderDimBackground(g);

        if (playerClass == null) {
            g.drawCenteredString(font, Component.translatable("gui.archetype.no_class"), width / 2, height / 2, 0xFF5555);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        // Smooth scroll interpolation
        smoothScrollOffset += (scrollOffset - smoothScrollOffset) * 0.35f;
        if (Math.abs(smoothScrollOffset - scrollOffset) < 0.5f) smoothScrollOffset = scrollOffset;
        smoothLeftScrollOffset += (leftScrollOffset - smoothLeftScrollOffset) * 0.35f;
        if (Math.abs(smoothLeftScrollOffset - leftScrollOffset) < 0.5f) smoothLeftScrollOffset = leftScrollOffset;

        if (tabTransition < 1f) {
            tabTransition = Math.min(1f, tabTransition + 0.08f);
        }

        ClientClassData data = ClientClassData.getInstance();
        int classColor = playerClass.getColor();

        long elapsed = System.currentTimeMillis() - openTime;
        float openAlpha = Mth.clamp(elapsed / 200f, 0f, 1f);

        // 16:9 panel
        int panelH = Math.min(height - 24, (int) ((width - 40) * 9.0 / 16.0));
        int panelW = (int) (panelH * 16.0 / 9.0);
        if (panelW > width - 24) {
            panelW = width - 24;
            panelH = (int) (panelW * 9.0 / 16.0);
        }
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        var pose = g.pose();
        if (openAlpha < 1f) {
            float scale = 0.95f + 0.05f * openAlpha;
            pose.pushPose();
            pose.translate(width / 2f, height / 2f, 0);
            pose.scale(scale, scale, 1f);
            pose.translate(-width / 2f, -height / 2f, 0);
        }

        renderPanel(g, panelX, panelY, panelW, panelH, classColor);

        int leftWidth = (int) ((panelW - PANEL_PADDING * 3 - DIVIDER_WIDTH) * 0.35);
        int rightWidth = panelW - PANEL_PADDING * 3 - DIVIDER_WIDTH - leftWidth;
        int leftX = panelX + PANEL_PADDING;
        int dividerX = leftX + leftWidth + PANEL_PADDING;
        int rightX = dividerX + DIVIDER_WIDTH + PANEL_PADDING;
        int contentTop = panelY + PANEL_PADDING;
        int contentBottom = panelY + panelH - PANEL_PADDING;

        renderVerticalDivider(g, dividerX, contentTop + 4, contentBottom - 4, classColor);

        this.cachedLeftX = leftX;
        this.cachedDividerX = dividerX;
        this.cachedContentTop = contentTop;
        this.cachedContentBottom = contentBottom;
        this.cachedRightX = rightX;
        this.cachedRightWidth = rightWidth;

        int tabHeaderHeight = font.lineHeight + 8;
        this.cachedTabHeaderHeight = tabHeaderHeight;

        // ---- LEFT COLUMN ----
        int ly = contentTop;

        // 3D player model — smoothly follows cursor
        int modelSpaceH = 70;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int modelCenterX = leftX + leftWidth / 2;
            int modelBottom = contentTop + modelSpaceH;
            renderPlayerModel(g, mc.player, modelCenterX, modelBottom, 35, mouseX, mouseY);
        }
        ly += modelSpaceH + 8;

        // Class name
        pose.pushPose();
        float nameScale = 1.6f;
        Component name = Component.translatable(playerClass.getNameKey());
        int namePixelW = (int) (font.width(name) * nameScale);
        float nameX = leftX + (leftWidth - namePixelW) / 2f;
        pose.translate(nameX, ly, 0);
        pose.scale(nameScale, nameScale, 1);
        g.drawString(font, name, 0, 0, 0xFF000000 | classColor, false);
        pose.popPose();
        ly += (int) (font.lineHeight * nameScale) + 8;

        // Lore
        if (!playerClass.getLoreKeys().isEmpty()) {
            Component lore = Component.translatable(playerClass.getLoreKeys().get(0))
                    .withStyle(s -> s.withItalic(true).withColor(0x707070));
            List<FormattedCharSequence> loreLines = font.split(lore, leftWidth);
            for (FormattedCharSequence line : loreLines) {
                int lw = font.width(line);
                g.drawString(font, line, leftX + (leftWidth - lw) / 2, ly, 0x707070, false);
                ly += font.lineHeight + 1;
            }
            ly += 8;
        }

        renderHorizontalSeparator(g, leftX, ly, leftWidth, classColor);
        ly += SECTION_GAP;

        // Level + XP
        int level = data.getLevel();
        int xp = data.getExperience();
        int neededXp = PlayerClassData.experienceForLevel(level + 1, 100);
        float xpProgress = neededXp > 0 ? (float) xp / neededXp : 0;

        if (smoothLevelProgress < 0) smoothLevelProgress = 0f;
        smoothLevelProgress += (xpProgress - smoothLevelProgress) * 0.1f;
        if (Math.abs(smoothLevelProgress - xpProgress) < 0.005f) smoothLevelProgress = xpProgress;

        Component levelText = Component.translatable("gui.archetype.level", level);
        g.drawString(font, levelText, leftX, ly, 0xFFCC88, false);
        ly += 12;

        int barW = leftWidth;
        this.levelBarX = leftX;
        this.levelBarY = ly;
        this.levelBarW = barW;
        boolean barHover = mouseX >= leftX && mouseX <= leftX + barW
                && mouseY >= ly && mouseY <= ly + 20;

        renderAnimatedProgressBar(g, leftX, ly, barW, 6, smoothLevelProgress,
                barHover ? 0x66DD66 : 0x44CC44, classColor);
        ly += 14;

        // ---- Left scrollable area ----
        int leftScrollTop = ly;
        this.cachedLeftScrollTop = leftScrollTop;
        g.enableScissor(leftX, leftScrollTop, dividerX - PANEL_PADDING, contentBottom);
        ly -= (int) smoothLeftScrollOffset;

        // Resource
        if (playerClass.getResource() != null) {
            var res = playerClass.getResource();
            float current = data.getResourceCurrent();
            float max = res.maxValue();

            renderHorizontalSeparator(g, leftX, ly, leftWidth, classColor);
            ly += SECTION_GAP;

            Component resName = Component.translatable(res.typeKey());
            g.drawString(font, resName, leftX, ly, 0xFF000000 | res.color(), false);
            ly += 12;
            ClassScreenRenderer.renderProgressBar(g, leftX, ly, barW, 8, current / max, res.color());
            ly += 10;
            String resStr = String.format("%.0f/%.0f", current, max);
            g.drawString(font, resStr, leftX + (barW - font.width(resStr)) / 2, ly, 0xFF000000 | res.color(), false);
            ly += 14;
        }

        // Attributes
        if (!playerClass.getAttributes().isEmpty()) {
            renderHorizontalSeparator(g, leftX, ly, leftWidth, classColor);
            ly += SECTION_GAP;

            renderSectionLabel(g, "gui.archetype.attributes", leftX, ly, leftWidth);
            ly += font.lineHeight + 4;

            for (AttributeModifierEntry attr : playerClass.getAttributes()) {
                double baseValue = getBaseValue(attr.attribute().toString());
                double scalingBonus = getXpScalingBonus(attr.attribute(), level);
                if (Math.abs(attr.value()) < 0.001) continue;
                String attrName = Component.translatable("gui.archetype.attr." + attr.attribute().getPath().replace("generic.", "")).getString();
                double value = baseValue + attr.value() + scalingBonus;
                int attrBarW = Math.min(80, leftWidth / 3);
                ClassScreenRenderer.renderAttributeBar(g, font, attrName, value, baseValue, leftX, ly, attrBarW);
                ly += 13;
            }
        }

        // Ability stats
        if (!playerClass.getAbilityStats().isEmpty()) {
            renderHorizontalSeparator(g, leftX, ly, leftWidth, classColor);
            ly += SECTION_GAP - 4;

            for (PlayerClass.AbilityStatEntry stat : playerClass.getAbilityStats()) {
                if ("header".equals(stat.format())) {
                    ly += 2;
                    Component headerText = Component.translatable(stat.nameKey())
                            .withStyle(Style.EMPTY.withBold(true));
                    g.drawString(font, headerText, leftX, ly, brightenColor(classColor, 0.45f), false);
                    int hw = font.width(headerText);
                    g.fill(leftX + hw + 4, ly + font.lineHeight / 2, leftX + leftWidth, ly + font.lineHeight / 2 + 1, 0x20FFFFFF);
                    ly += font.lineHeight + 3;
                    continue;
                }
                Component statName = Component.translatable(stat.nameKey());
                String statValue = stat.formatValue(level);
                int valueColor;
                if ("boolean".equals(stat.format())) {
                    valueColor = stat.computeValue(level) > 0 ? 0x44CC44 : 0xCC4444;
                } else {
                    valueColor = 0xCCCCCC;
                }
                int nameW = font.width(statName);
                int valueW = font.width(statValue);
                if (nameW + valueW + 6 > leftWidth) {
                    g.drawString(font, statName, leftX, ly, 0x999999, false);
                    ly += font.lineHeight + 1;
                    g.drawString(font, statValue, leftX + leftWidth - valueW, ly, valueColor, false);
                    ly += font.lineHeight + 1;
                } else {
                    g.drawString(font, statName, leftX, ly, 0x999999, false);
                    g.drawString(font, statValue, leftX + leftWidth - valueW, ly, valueColor, false);
                    ly += 11;
                }
            }
        }

        leftContentHeight = ly + smoothLeftScrollOffset - leftScrollTop + PANEL_PADDING;
        g.disableScissor();

        float leftViewH = contentBottom - leftScrollTop;
        if (leftContentHeight > leftViewH) {
            renderLeftScrollbar(g, dividerX - PANEL_PADDING - SCROLLBAR_WIDTH, leftScrollTop, leftViewH, classColor);
        }

        // ---- RIGHT COLUMN ----
        int tabY = contentTop;
        Component abilitiesTab = Component.translatable("gui.archetype.abilities");
        Component passivesTab = Component.translatable("gui.archetype.passives");

        int tab0X = rightX;
        int tab1X = rightX + rightWidth / 2;
        int tabW = rightWidth / 2;

        boolean hoverTab0 = mouseX >= tab0X && mouseX < tab0X + tabW
                && mouseY >= tabY && mouseY < tabY + tabHeaderHeight;
        boolean hoverTab1 = mouseX >= tab1X && mouseX < tab1X + tabW
                && mouseY >= tabY && mouseY < tabY + tabHeaderHeight;

        // Tab backgrounds
        g.fill(tab0X, tabY, tab0X + tabW, tabY + tabHeaderHeight,
                selectedTab == 0 ? 0x30FFFFFF : (hoverTab0 ? 0x20FFFFFF : 0x10FFFFFF));
        g.fill(tab1X, tabY, tab1X + tabW, tabY + tabHeaderHeight,
                selectedTab == 1 ? 0x30FFFFFF : (hoverTab1 ? 0x20FFFFFF : 0x10FFFFFF));

        int tab0Color = selectedTab == 0 ? (0xFF000000 | classColor) : (hoverTab0 ? 0xBBBBBB : 0x888888);
        int tab1Color = selectedTab == 1 ? (0xFF000000 | classColor) : (hoverTab1 ? 0xBBBBBB : 0x888888);
        g.drawCenteredString(font, abilitiesTab, tab0X + tabW / 2, tabY + (tabHeaderHeight - font.lineHeight) / 2, tab0Color);
        g.drawCenteredString(font, passivesTab, tab1X + tabW / 2, tabY + (tabHeaderHeight - font.lineHeight) / 2, tab1Color);

        // Underline
        int ulX = selectedTab == 0 ? tab0X : tab1X;
        g.fill(ulX, tabY + tabHeaderHeight - 2, ulX + tabW, tabY + tabHeaderHeight, 0xFF000000 | classColor);

        // Right content
        int rightContentTop = contentTop + tabHeaderHeight + 6;
        g.enableScissor(rightX, rightContentTop, rightX + rightWidth, contentBottom);

        int ry = rightContentTop - (int) smoothScrollOffset;

        if (selectedTab == 0) {
            ry = renderAbilitiesTab(g, rightX, ry, rightWidth, data, classColor, level, mouseX, mouseY);
        } else {
            ry = renderPassivesTab(g, rightX, ry, rightWidth, classColor, mouseX, mouseY);
        }

        contentHeight = ry + smoothScrollOffset - rightContentTop + PANEL_PADDING;
        g.disableScissor();

        float viewH = contentBottom - rightContentTop;
        if (contentHeight > viewH) {
            renderScrollbar(g, rightX + rightWidth - SCROLLBAR_WIDTH - 2, rightContentTop, viewH, classColor);
        }

        if (openAlpha < 1f) {
            pose.popPose();
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (playerClass != null && mouseX >= levelBarX && mouseX <= levelBarX + levelBarW
                && mouseY >= levelBarY && mouseY <= levelBarY + 20) {
            renderLevelTooltip(g, mouseX, mouseY);
        }
    }

    // ---- Abilities tab ----

    private int renderAbilitiesTab(GuiGraphics g, int x, int ry, int rw,
                                   ClientClassData data, int classColor, int level,
                                   int mouseX, int mouseY) {
        if (playerClass.getActiveAbilities().isEmpty()) return ry;

        for (ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
            int slotIdx = switch (ability.slot()) {
                case "ability_1" -> 0;
                case "ability_2" -> 1;
                case "ability_3" -> 2;
                default -> 0;
            };
            String key = ArchetypeKeybinds.getSlotKeyDisplay(slotIdx);
            boolean locked = ability.unlockLevel() > data.getLevel();

            Component desc = Component.translatable(ability.descriptionKey());
            List<FormattedCharSequence> descLines = font.split(desc, rw - 42);
            int descH = descLines.size() * (font.lineHeight + 1);

            // Card dimensions
            int cardLeft = x;
            int cardRight = x + rw - 6;
            int cardTop = ry;
            int cardBottom = ry + 10 + font.lineHeight + 4 + descH + 8;

            boolean hover = mouseX >= cardLeft && mouseX < cardRight
                    && mouseY >= cardTop && mouseY < cardBottom;

            // Card background — gradient with subtle class color tint
            int bgTop = locked ? 0x20222222 : (hover ? 0x30FFFFFF : 0x20FFFFFF);
            int bgBot = locked ? 0x10111111 : (hover ? 0x18FFFFFF : 0x0CFFFFFF);
            g.fillGradient(cardLeft, cardTop, cardRight, cardBottom, bgTop, bgBot);

            // Left accent strip (3px wide, class-colored)
            int accentAlpha = locked ? 0x30 : (hover ? 0xA0 : 0x60);
            g.fill(cardLeft, cardTop, cardLeft + 3, cardBottom, (accentAlpha << 24) | (classColor & 0x00FFFFFF));

            // Bottom border line
            g.fill(cardLeft + 3, cardBottom - 1, cardRight, cardBottom, 0x10FFFFFF);

            // Keybind badge — small pill shape
            int badgeX = cardLeft + 8;
            int badgeY = cardTop + 8;
            int badgeW = font.width(key) + 8;
            int badgeH = font.lineHeight + 4;
            int badgeBg = locked ? 0x60333333 : (0xB0000000 | (classColor & 0x00FFFFFF));
            g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, badgeBg);
            g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 1, 0x20FFFFFF); // top highlight
            int keyColor = locked ? 0x666666 : 0xEEEEEE;
            g.drawString(font, key, badgeX + 4, badgeY + 2, keyColor, false);

            // Ability name
            int nameX = badgeX + badgeW + 6;
            int nameColor = locked ? 0x666666 : 0xFFFFFF;
            g.drawString(font, Component.translatable(ability.nameKey()), nameX, badgeY + 2, nameColor, false);

            // Lock indicator on the right
            if (locked) {
                Component lockText = Component.translatable("gui.archetype.locked", ability.unlockLevel());
                int lockW = font.width(lockText);
                g.drawString(font, lockText, cardRight - lockW - 6, badgeY + 2, 0x555555, false);
            }

            // Slot label (small, top-right)
            String slotLabel = ability.slot().replace("ability_", "#");
            int slotLabelW = font.width(slotLabel);
            g.drawString(font, slotLabel, cardRight - slotLabelW - 6, badgeY + 2, 0x404050, false);

            // Description
            int descY = badgeY + badgeH + 4;
            int descColor = locked ? 0x555555 : 0x999999;
            for (FormattedCharSequence line : descLines) {
                g.drawString(font, line, cardLeft + 10, descY, descColor, false);
                descY += font.lineHeight + 1;
            }

            ry = cardBottom + 4;
        }

        // Extra sections
        for (ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
            for (PlayerClass.ExtraAbilitySection section : playerClass.getExtraAbilitySections()) {
                if (section.parentSlot().equals(ability.slot())) {
                    ry = renderExtraAbilitySection(g, x, ry, rw, section, classColor, level, mouseX, mouseY);
                }
            }
        }
        ry += 4;

        return ry;
    }

    // ---- Passives tab ----

    private int renderPassivesTab(GuiGraphics g, int x, int ry, int rw,
                                  int classColor, int mouseX, int mouseY) {
        if (playerClass.getPassiveAbilities().isEmpty()) return ry;

        var sorted = playerClass.getPassiveAbilities().stream()
                .filter(p -> !p.hidden())
                .sorted((a, b) -> Boolean.compare(b.positive(), a.positive()))
                .toList();

        for (PassiveAbilityEntry passive : sorted) {
            boolean positive = passive.positive();
            int accentColor = positive ? 0x44CC44 : 0xCC4444;

            Component pDesc = Component.translatable(passive.descriptionKey());
            List<FormattedCharSequence> descLines = font.split(pDesc, rw - 28);
            int descH = descLines.size() * (font.lineHeight + 1);

            // Card dimensions
            int cardLeft = x;
            int cardRight = x + rw - 6;
            int cardTop = ry;
            int cardBottom = ry + 8 + font.lineHeight + 4 + descH + 6;

            boolean hover = mouseX >= cardLeft && mouseX < cardRight
                    && mouseY >= cardTop && mouseY < cardBottom;

            // Background — different tint for positive/negative
            int bgBase = positive ? 0x003300 : 0x330000;
            int bgAlphaTop = hover ? 0x28 : 0x18;
            int bgAlphaBot = hover ? 0x18 : 0x0C;
            g.fillGradient(cardLeft, cardTop, cardRight, cardBottom,
                    (bgAlphaTop << 24) | bgBase, (bgAlphaBot << 24) | bgBase);

            // Left accent strip (3px)
            int stripAlpha = hover ? 0xC0 : 0x80;
            g.fill(cardLeft, cardTop, cardLeft + 3, cardBottom, (stripAlpha << 24) | (accentColor & 0x00FFFFFF));

            // Bottom border
            g.fill(cardLeft + 3, cardBottom - 1, cardRight, cardBottom, 0x08FFFFFF);

            // Marker icon — circle-ish indicator
            int iconX = cardLeft + 10;
            int iconY = cardTop + 8;
            String marker = positive ? "\u25B2" : "\u25BC"; // ▲ / ▼
            g.drawString(font, marker, iconX, iconY, accentColor, false);

            // Passive name
            int nameX = iconX + font.width(marker) + 4;
            g.drawString(font, Component.translatable(passive.nameKey()), nameX, iconY, accentColor, false);

            // Description
            int descY = iconY + font.lineHeight + 4;
            for (FormattedCharSequence line : descLines) {
                g.drawString(font, line, cardLeft + 10, descY, 0x999999, false);
                descY += font.lineHeight + 1;
            }

            ry = cardBottom + 3;
        }

        return ry;
    }

    // ---- Player model ----

    private void renderPlayerModel(GuiGraphics g, LivingEntity entity, int x, int y, int modelScale,
                                   int mouseX, int mouseY) {
        // Target offset from model center to cursor
        float targetDx = (float) x - mouseX;
        float targetDy = (float) (y - modelScale) - mouseY;

        // Smooth interpolation — slow, gentle follow
        smoothModelDx += (targetDx - smoothModelDx) * 0.08f;
        smoothModelDy += (targetDy - smoothModelDy) * 0.08f;

        // Clamp rotation range so model doesn't spin wildly
        float clampedDx = Mth.clamp(smoothModelDx, -80f, 80f);
        float clampedDy = Mth.clamp(smoothModelDy, -50f, 50f);

        // Reduced multipliers for gentler rotation
        Quaternionf entityPose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf().rotateX(clampedDy * 8f * ((float) Math.PI / 180f));

        float origBodyRot = entity.yBodyRot;
        float origYRot = entity.getYRot();
        float origXRot = entity.getXRot();
        float origHeadRotO = entity.yHeadRotO;
        float origHeadRot = entity.yHeadRot;

        entity.yBodyRot = 180f + clampedDx * 8f;
        entity.setYRot(180f + clampedDx * 12f);
        entity.setXRot(-clampedDy * 8f);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        InventoryScreen.renderEntityInInventory(g, x, y, modelScale, entityPose, cameraOrientation, entity);

        entity.yBodyRot = origBodyRot;
        entity.setYRot(origYRot);
        entity.setXRot(origXRot);
        entity.yHeadRotO = origHeadRotO;
        entity.yHeadRot = origHeadRot;
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= cachedRightX && mouseX < cachedRightX + cachedRightWidth
                && mouseY >= cachedContentTop && mouseY < cachedContentTop + cachedTabHeaderHeight) {
            int newTab = (mouseX < cachedRightX + cachedRightWidth / 2) ? 0 : 1;
            if (newTab != selectedTab) {
                selectedTab = newTab;
                scrollOffset = 0;
                smoothScrollOffset = 0;
                tabTransition = 0f;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= cachedLeftX && mouseX < cachedDividerX) {
            float leftViewH = cachedContentBottom - cachedLeftScrollTop;
            leftScrollOffset -= (float) (delta * 20);
            leftScrollOffset = Mth.clamp(leftScrollOffset, 0, Math.max(0, leftContentHeight - leftViewH));
        } else {
            float viewH = cachedContentBottom - cachedContentTop;
            scrollOffset -= (float) (delta * 20);
            scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - viewH));
        }
        return true;
    }

    // ---- Rendering helpers ----

    private void renderDimBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, int classColor) {
        g.fillGradient(x, y, x + w, y + h, 0xFF181828, 0xFF121220);

        int borderColor = 0xCC000000 | classColor;
        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);

        int accentFull = 0xFF000000 | classColor;
        int accentHalf = 0x60000000 | classColor;
        int mid = x + w / 2;
        g.fillGradient(x + 1, y, mid, y + 2, accentHalf, accentFull);
        g.fillGradient(mid, y, x + w - 1, y + 2, accentFull, accentHalf);

        g.fillGradient(x + 1, y + 2, x + w - 1, y + 12, 0x18000000 | classColor, 0x00000000);
    }

    private void renderVerticalDivider(GuiGraphics g, int x, int top, int bottom, int classColor) {
        int mid = (top + bottom) / 2;
        int quarterH = (bottom - top) / 4;
        g.fillGradient(x, top, x + DIVIDER_WIDTH, mid - quarterH, 0x08000000 | classColor, 0x18000000 | classColor);
        g.fill(x, mid - quarterH, x + DIVIDER_WIDTH, mid + quarterH, 0x50000000 | classColor);
        g.fillGradient(x, mid + quarterH, x + DIVIDER_WIDTH, bottom, 0x18000000 | classColor, 0x08000000 | classColor);
    }

    private void renderHorizontalSeparator(GuiGraphics g, int x, int y, int w, int classColor) {
        int mid = x + w / 2;
        int core = w / 6;
        g.fill(mid - core, y, mid + core, y + 1, 0x50000000 | classColor);
        g.fill(x, y, mid - core, y + 1, 0x18000000 | classColor);
        g.fill(mid + core, y, x + w, y + 1, 0x18000000 | classColor);
    }

    private void renderSectionLabel(GuiGraphics g, String titleKey, int x, int y, int w) {
        Component title = Component.translatable(titleKey).withStyle(Style.EMPTY.withColor(0x909090));
        g.drawString(font, title, x, y, 0x909090, false);
        int tw = font.width(title);
        g.fill(x + tw + 6, y + font.lineHeight / 2, x + w, y + font.lineHeight / 2 + 1, 0x1CFFFFFF);
    }

    private void renderAnimatedProgressBar(GuiGraphics g, int x, int y, int barWidth, int barHeight,
                                           float progress, int color, int classColor) {
        g.fill(x, y, x + barWidth, y + barHeight, 0x25FFFFFF);

        int fillWidth = (int) (barWidth * Mth.clamp(progress, 0f, 1f));
        if (fillWidth > 0) {
            int baseColor = 0xFF000000 | color;
            int lighterColor = brightenColor(color, 0.3f);
            g.fillGradient(x, y, x + fillWidth, y + barHeight, lighterColor, baseColor);
            g.fill(x, y, x + fillWidth, y + 1, 0x30FFFFFF);

            long time = System.currentTimeMillis();
            float shimmerPos = (float) ((time % 3000) / 3000.0);
            int shimmerX = x + (int) (shimmerPos * barWidth);
            int shimmerWidth = 8;
            if (shimmerX < x + fillWidth && shimmerX + shimmerWidth > x) {
                int sx1 = Math.max(shimmerX, x);
                int sx2 = Math.min(shimmerX + shimmerWidth, x + fillWidth);
                g.fill(sx1, y, sx2, y + barHeight, 0x18FFFFFF);
            }
        }
    }

    private int renderExtraAbilitySection(GuiGraphics g, int x, int y, int contentWidth,
                                          PlayerClass.ExtraAbilitySection section, int classColor, int playerLevel,
                                          int mouseX, int mouseY) {
        boolean locked = section.unlockLevel() > 0 && playerLevel < section.unlockLevel();

        Component sectionName = Component.translatable(section.nameKey());
        int nameWidth = font.width(sectionName);
        int centerX = x + contentWidth / 2;
        int textStartX = centerX - nameWidth / 2 - 6;
        int textEndX = centerX + nameWidth / 2 + 6;
        int titleColor = locked ? 0x555555 : (0xFF000000 | classColor);

        g.fill(x + 8, y + font.lineHeight / 2, textStartX, y + font.lineHeight / 2 + 1, 0x30000000 | classColor);
        g.drawString(font, sectionName, centerX - nameWidth / 2, y, titleColor, false);
        g.fill(textEndX, y + font.lineHeight / 2, x + contentWidth - 8, y + font.lineHeight / 2 + 1, 0x30000000 | classColor);
        y += font.lineHeight + 6;

        if (locked) {
            int cardTop = y;
            int entryH = section.entries().size() * (font.lineHeight + 4) + CARD_PAD * 2;
            int cardBottom = cardTop + entryH;
            g.fill(x, cardTop, x + contentWidth - 8, cardBottom, 0x30000000);

            Component lockMsg = Component.translatable("gui.archetype.locked", section.unlockLevel());
            int lockMsgW = font.width(lockMsg);
            g.drawString(font, lockMsg, centerX - lockMsgW / 2, cardTop + entryH / 2 - font.lineHeight / 2, 0x555555, false);

            y = cardBottom + 4;
        } else {
            for (PlayerClass.ExtraAbilityEntry entry : section.entries()) {
                Component entryName = Component.translatable(entry.nameKey());
                Component entryDesc = Component.translatable(entry.descriptionKey());

                List<FormattedCharSequence> descLines = font.split(entryDesc, contentWidth - 28);
                int totalH = font.lineHeight + descLines.size() * (font.lineHeight + 1) + 4;

                boolean entryHover = mouseX >= x + 4 && mouseX < x + contentWidth - 12
                        && mouseY >= y - 2 && mouseY < y + totalH;

                int bgAlpha = entryHover ? 0x20 : 0x10;
                g.fill(x + 4, y - 2, x + contentWidth - 12, y + totalH, (bgAlpha << 24) | (classColor & 0x00FFFFFF));
                g.fill(x + 4, y - 2, x + 7, y + totalH, 0x40000000 | classColor);

                g.drawString(font, entryName, x + 12, y, 0xDDDDDD, false);
                y += font.lineHeight + 1;

                for (FormattedCharSequence line : descLines) {
                    g.drawString(font, line, x + 12, y, 0x888888, false);
                    y += font.lineHeight + 1;
                }
                y += 4;
            }
        }
        return y;
    }

    private void renderScrollbar(GuiGraphics g, int x, int top, float viewH, int classColor) {
        float maxScroll = Math.max(1, contentHeight - viewH);
        float thumbRatio = viewH / contentHeight;
        int thumbH = Math.max(10, (int) (viewH * thumbRatio));
        int thumbY = (int) top + (int) ((viewH - thumbH) * (smoothScrollOffset / maxScroll));
        g.fill(x, (int) top, x + SCROLLBAR_WIDTH, (int) (top + viewH), 0x10FFFFFF);
        g.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, 0x80000000 | classColor);
    }

    private void renderLeftScrollbar(GuiGraphics g, int x, int top, float viewH, int classColor) {
        float maxScroll = Math.max(1, leftContentHeight - viewH);
        float thumbRatio = viewH / leftContentHeight;
        int thumbH = Math.max(10, (int) (viewH * thumbRatio));
        int thumbY = (int) top + (int) ((viewH - thumbH) * (smoothLeftScrollOffset / maxScroll));
        g.fill(x, (int) top, x + SCROLLBAR_WIDTH, (int) (top + viewH), 0x10FFFFFF);
        g.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, 0x80000000 | classColor);
    }

    private void renderLevelTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ClientClassData data = ClientClassData.getInstance();
        int level = data.getLevel();
        int xp = data.getExperience();
        int neededXp = PlayerClassData.experienceForLevel(level + 1, 100);

        List<PlayerClass.LevelMilestone> progression = playerClass.getProgression();
        int maxLevel = progression.isEmpty() ? 0 : progression.get(progression.size() - 1).level();

        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable("gui.archetype.level", level)
                .append(Component.literal(" / " + maxLevel).withStyle(Style.EMPTY.withColor(0x666666))));

        if (level < maxLevel) {
            int pct = neededXp > 0 ? (int) ((float) xp / neededXp * 100) : 0;
            lines.add(Component.literal(xp + " / " + neededXp + " XP (" + pct + "%)")
                    .withStyle(Style.EMPTY.withColor(0x888888)));
        } else {
            lines.add(Component.translatable("gui.archetype.max_level")
                    .withStyle(Style.EMPTY.withColor(0xFFCC88)));
        }

        for (PlayerClass.ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
            int unlock = ability.unlockLevel();
            if (unlock > 1) {
                Component abilityName = Component.translatable(ability.nameKey());
                if (unlock > level) {
                    lines.add(Component.translatable("gui.archetype.level_unlock", unlock)
                            .withStyle(Style.EMPTY.withColor(0xCC4444))
                            .append(Component.literal(" "))
                            .append(abilityName.copy().withStyle(Style.EMPTY.withColor(0xAAAAAA))));
                } else {
                    lines.add(Component.literal("\u2714 ").withStyle(Style.EMPTY.withColor(0x44CC44))
                            .append(abilityName.copy().withStyle(Style.EMPTY.withColor(0x44CC44))));
                }
            }
        }

        if (!progression.isEmpty()) {
            PlayerClass.LevelMilestone nextMilestone = null;
            for (PlayerClass.LevelMilestone milestone : progression) {
                if (milestone.level() > level) {
                    nextMilestone = milestone;
                    break;
                }
            }
            if (nextMilestone != null) {
                lines.add(Component.empty());
                lines.add(Component.translatable("gui.archetype.next_level")
                        .withStyle(Style.EMPTY.withColor(0xFFCC88)));
                lines.add(Component.translatable("gui.archetype.level_unlock", nextMilestone.level())
                        .withStyle(Style.EMPTY.withColor(0x888888)));
                Component desc = Component.translatable(nextMilestone.descriptionKey());
                lines.add(desc.copy().withStyle(Style.EMPTY.withColor(0xAAAAAA)));
            }
        }

        int maxRawW = 0;
        for (Component line : lines) {
            maxRawW = Math.max(maxRawW, font.width(line));
        }
        int contentW = Math.min(maxRawW, 200);
        int tipW = contentW + 12;

        List<FormattedCharSequence> rendered = new ArrayList<>();
        for (Component line : lines) {
            if (line.getString().isEmpty()) {
                rendered.add(null);
            } else {
                rendered.addAll(font.split(line, contentW));
            }
        }

        int lineH = font.lineHeight + 2;
        int tipH = 8;
        for (FormattedCharSequence seq : rendered) {
            tipH += seq == null ? 4 : lineH;
        }

        int tipX = mouseX + 12;
        int tipY = mouseY - 8;
        if (tipX + tipW > width - 4) tipX = mouseX - tipW - 4;
        if (tipY + tipH > height - 4) tipY = height - tipH - 4;
        if (tipY < 4) tipY = 4;

        g.fillGradient(tipX, tipY, tipX + tipW, tipY + tipH, 0xFF121220, 0xFF0E0E18);
        int classColor = playerClass.getColor();
        int bc = 0xCC000000 | classColor;
        g.fill(tipX, tipY, tipX + tipW, tipY + 1, bc);
        g.fill(tipX, tipY + tipH - 1, tipX + tipW, tipY + tipH, bc);
        g.fill(tipX, tipY, tipX + 1, tipY + tipH, bc);
        g.fill(tipX + tipW - 1, tipY, tipX + tipW, tipY + tipH, bc);

        int textY = tipY + 4;
        for (FormattedCharSequence seq : rendered) {
            if (seq == null) {
                textY += 4;
                continue;
            }
            g.drawString(font, seq, tipX + 6, textY, 0xFFFFFF, false);
            textY += lineH;
        }
    }

    // ---- Data helpers ----

    private double getXpScalingBonus(ResourceLocation attributeId, int classLevel) {
        double totalBonus = 0;
        for (PlayerClass.PassiveAbilityEntry passive : playerClass.getPassiveAbilities()) {
            if (passive.type().getPath().equals("xp_attribute_scaling")) {
                JsonObject p = passive.params();
                int targetLevel = p.has("target_level") ? p.get("target_level").getAsInt() : 40;
                float ratio = Math.min(1.0f, (float) classLevel / targetLevel);
                String attrPath = attributeId.getPath();
                if (attrPath.contains("max_health")) {
                    float bonus = p.has("health_bonus") ? p.get("health_bonus").getAsFloat() : 0;
                    int threshold = p.has("health_threshold") ? p.get("health_threshold").getAsInt() : 0;
                    totalBonus += threshold > 0
                            ? (classLevel >= threshold ? bonus : 0)
                            : bonus * ratio;
                }
                if (attrPath.contains("attack_speed")) {
                    float bonus = p.has("attack_speed_bonus") ? p.get("attack_speed_bonus").getAsFloat() : 0;
                    int threshold = p.has("attack_speed_threshold") ? p.get("attack_speed_threshold").getAsInt() : 0;
                    totalBonus += threshold > 0
                            ? (classLevel >= threshold ? bonus : 0)
                            : bonus * ratio;
                }
                if (attrPath.contains("attack_damage")) {
                    float bonus = p.has("attack_damage_bonus") ? p.get("attack_damage_bonus").getAsFloat() : 0;
                    int threshold = p.has("attack_damage_threshold") ? p.get("attack_damage_threshold").getAsInt() : 0;
                    totalBonus += threshold > 0
                            ? (classLevel >= threshold ? bonus : 0)
                            : bonus * ratio;
                }
            }
        }
        return totalBonus;
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

    private static int brightenColor(int color, float t) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * (1 - t) + 255 * t));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * (1 - t) + 255 * t));
        int b = Math.min(255, (int) ((color & 0xFF) * (1 - t) + 255 * t));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
