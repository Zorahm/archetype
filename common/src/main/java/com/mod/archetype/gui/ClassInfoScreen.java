package com.mod.archetype.gui;

import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import com.mod.archetype.core.PlayerClass.AttributeModifierEntry;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ClassInfoScreen extends Screen {

    private PlayerClass playerClass;
    private float scrollOffset = 0;
    private float contentHeight = 0;

    private static final int PANEL_PADDING = 14;
    private static final int SECTION_GAP = 12;
    private static final int CARD_PAD = 6;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int DIVIDER_WIDTH = 1;
    private int levelBarX, levelBarY, levelBarW;

    public ClassInfoScreen() {
        super(Component.translatable("gui.archetype.class_info"));
    }

    @Override
    protected void init() {
        ClientClassData data = ClientClassData.getInstance();
        if (data.hasClass() && data.getClassId() != null) {
            playerClass = ClassRegistry.getInstance().get(data.getClassId()).orElse(null);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dark vignette background
        renderDimBackground(g);

        if (playerClass == null) {
            g.drawCenteredString(font, Component.translatable("gui.archetype.no_class"), width / 2, height / 2, 0xFF5555);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        ClientClassData data = ClientClassData.getInstance();
        int classColor = playerClass.getColor();

        // 16:9 panel
        int panelH = Math.min(height - 24, (int) ((width - 40) * 9.0 / 16.0));
        int panelW = (int) (panelH * 16.0 / 9.0);
        if (panelW > width - 24) {
            panelW = width - 24;
            panelH = (int) (panelW * 9.0 / 16.0);
        }
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        // Draw panel
        renderPanel(g, panelX, panelY, panelW, panelH, classColor);

        // Split into left (35%) and right (65%) columns
        int leftWidth = (int) ((panelW - PANEL_PADDING * 3 - DIVIDER_WIDTH) * 0.35);
        int rightWidth = panelW - PANEL_PADDING * 3 - DIVIDER_WIDTH - leftWidth;
        int leftX = panelX + PANEL_PADDING;
        int dividerX = leftX + leftWidth + PANEL_PADDING;
        int rightX = dividerX + DIVIDER_WIDTH + PANEL_PADDING;
        int contentTop = panelY + PANEL_PADDING;
        int contentBottom = panelY + panelH - PANEL_PADDING;

        // Vertical divider
        renderVerticalDivider(g, dividerX, contentTop + 4, contentBottom - 4, classColor);

        // ---- LEFT COLUMN (no scroll) ----
        int ly = contentTop;

        // Class name (big)
        var pose = g.pose();
        pose.pushPose();
        float scale = 1.6f;
        Component name = Component.translatable(playerClass.getNameKey());
        int namePixelW = (int) (font.width(name) * scale);
        float nameX = leftX + (leftWidth - namePixelW) / 2f;
        pose.translate(nameX, ly, 0);
        pose.scale(scale, scale, 1);
        g.drawString(font, name, 0, 0, 0xFF000000 | classColor, false);
        pose.popPose();
        ly += (int) (font.lineHeight * scale) + 8;

        // Lore (word-wrapped, centered)
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

        // Separator
        renderHorizontalSeparator(g, leftX, ly, leftWidth, classColor);
        ly += SECTION_GAP;

        // Level + XP
        int level = data.getLevel();
        int xp = data.getExperience();
        int neededXp = PlayerClassData.experienceForLevel(level + 1, 100);
        float xpProgress = neededXp > 0 ? (float) xp / neededXp : 0;

        Component levelText = Component.translatable("gui.archetype.level", level);
        g.drawString(font, levelText, leftX, ly, 0xFFCC88, false);
        ly += 12;

        int barW = leftWidth;
        this.levelBarX = leftX;
        this.levelBarY = ly;
        this.levelBarW = barW;
        boolean barHover = mouseX >= leftX && mouseX <= leftX + barW
                && mouseY >= ly && mouseY <= ly + 20;
        ClassScreenRenderer.renderProgressBar(g, leftX, ly, barW, 6, xpProgress, barHover ? 0x66DD66 : 0x44CC44);
        ly += 14;

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
                if (Math.abs(attr.value() + scalingBonus) < 0.001) continue;
                String attrName = attr.attribute().getPath().replace("generic.", "").replace("_", " ");
                attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
                double value = baseValue + attr.value() + scalingBonus;
                int barWidth = Math.min(80, leftWidth / 3);
                ClassScreenRenderer.renderAttributeBar(g, font, attrName, value, baseValue, leftX, ly, barWidth);
                ly += 13;
            }
        }

        // Ability stats (dynamic, level-based)
        if (!playerClass.getAbilityStats().isEmpty()) {
            renderHorizontalSeparator(g, leftX, ly, leftWidth, classColor);
            ly += SECTION_GAP - 4;

            for (PlayerClass.AbilityStatEntry stat : playerClass.getAbilityStats()) {
                if ("header".equals(stat.format())) {
                    ly += 2;
                    Component headerText = Component.translatable(stat.nameKey());
                    g.drawString(font, headerText, leftX, ly, 0xFF000000 | classColor, false);
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
                g.drawString(font, statName, leftX, ly, 0x999999, false);
                g.drawString(font, statValue, leftX + leftWidth - font.width(statValue), ly, valueColor, false);
                ly += 11;
            }
        }

        // ---- RIGHT COLUMN (scrollable) ----
        g.enableScissor(rightX, contentTop, rightX + rightWidth, contentBottom);

        int ry = contentTop - (int) scrollOffset;

        // Active abilities
        if (!playerClass.getActiveAbilities().isEmpty()) {
            renderSectionLabel(g, "gui.archetype.abilities", rightX, ry, rightWidth);
            ry += font.lineHeight + 6;

            for (ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
                int slotIdx = switch (ability.slot()) {
                    case "ability_1" -> 0;
                    case "ability_2" -> 1;
                    case "ability_3" -> 2;
                    default -> 0;
                };
                String key = ArchetypeKeybinds.getSlotKeyDisplay(slotIdx);

                // Card
                Component desc = Component.translatable(ability.descriptionKey());
                List<FormattedCharSequence> descLines = font.split(desc, rightWidth - 36);
                int descH = descLines.size() * (font.lineHeight + 1);
                int cardTop = ry - CARD_PAD / 2;
                int cardBottom = ry + 12 + descH + CARD_PAD;
                g.fill(rightX, cardTop, rightX + rightWidth - 12, cardBottom, 0x1CFFFFFF);

                // Key badge
                String badge = "[" + key + "]";
                g.fill(rightX + 2, ry - 1, rightX + 2 + font.width(badge) + 4, ry + font.lineHeight + 1, 0xC0000000 | classColor);
                g.drawString(font, badge, rightX + 4, ry, 0xFFFFFF, false);

                // Name
                g.drawString(font, Component.translatable(ability.nameKey()), rightX + 28, ry, 0xFFFFFF, false);
                ry += 12;

                for (FormattedCharSequence line : descLines) {
                    g.drawString(font, line, rightX + 28, ry, 0x999999, false);
                    ry += font.lineHeight + 1;
                }
                ry += CARD_PAD + 2;

                // Extra ability sections for this slot
                for (PlayerClass.ExtraAbilitySection section : playerClass.getExtraAbilitySections()) {
                    if (section.parentSlot().equals(ability.slot())) {
                        ry = renderExtraAbilitySection(g, rightX, ry, rightWidth, section, classColor, level);
                    }
                }
            }
            ry += 4;
        }

        // Passives
        if (!playerClass.getPassiveAbilities().isEmpty()) {
            renderSectionLabel(g, "gui.archetype.passives", rightX, ry, rightWidth);
            ry += font.lineHeight + 6;

            var sorted = playerClass.getPassiveAbilities().stream()
                    .filter(p -> !p.hidden())
                    .sorted((a, b) -> Boolean.compare(b.positive(), a.positive()))
                    .toList();
            for (PassiveAbilityEntry passive : sorted) {
                boolean positive = passive.positive();
                int color = positive ? 0xFF44CC44 : 0xFFCC4444;
                String marker = positive ? "\u2714" : "\u2718";

                Component pDesc = Component.translatable(passive.descriptionKey());
                List<FormattedCharSequence> descLines = font.split(pDesc, rightWidth - 24);
                int descH = descLines.size() * (font.lineHeight + 1);
                int cardTop = ry - CARD_PAD / 2;
                int cardBottom = ry + 12 + descH + CARD_PAD;

                int bgColor = positive ? 0x18008800 : 0x18880000;
                g.fill(rightX, cardTop, rightX + rightWidth - 8, cardBottom, bgColor);
                g.fill(rightX, cardTop, rightX + 2, cardBottom, 0x80000000 | (color & 0x00FFFFFF));

                g.drawString(font, marker, rightX + 6, ry, color, false);
                g.drawString(font, Component.translatable(passive.nameKey()), rightX + 16, ry, color, false);
                ry += 12;

                for (FormattedCharSequence line : descLines) {
                    g.drawString(font, line, rightX + 16, ry, 0x999999, false);
                    ry += font.lineHeight + 1;
                }
                ry += CARD_PAD + 2;
            }
        }

        contentHeight = ry + scrollOffset - contentTop + PANEL_PADDING;
        g.disableScissor();

        // Scrollbar for right column
        float viewH = contentBottom - contentTop;
        if (contentHeight > viewH) {
            renderScrollbar(g, rightX + rightWidth - SCROLLBAR_WIDTH - 2, contentTop, viewH, classColor);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Level bar tooltip
        if (playerClass != null && mouseX >= levelBarX && mouseX <= levelBarX + levelBarW
                && mouseY >= levelBarY && mouseY <= levelBarY + 20) {
            renderLevelTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelH = Math.min(height - 24, (int) ((width - 40) * 9.0 / 16.0));
        int panelW = (int) (panelH * 16.0 / 9.0);
        if (panelW > width - 24) {
            panelW = width - 24;
            panelH = (int) (panelW * 9.0 / 16.0);
        }
        float viewH = panelH - PANEL_PADDING * 2;
        scrollOffset -= (float) (delta * 20);
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - viewH));
        return true;
    }

    // ---- Rendering helpers (shared style with ClassDetailScreen) ----

    private void renderDimBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, int classColor) {
        g.fill(x, y, x + w, y + h, 0xFF161622);
        int borderColor = 0xCC000000 | classColor;
        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);
        int accentColor = 0xFF000000 | classColor;
        g.fill(x + 1, y, x + w - 1, y + 2, accentColor);
    }

    private void renderVerticalDivider(GuiGraphics g, int x, int top, int bottom, int classColor) {
        int mid = (top + bottom) / 2;
        int quarterH = (bottom - top) / 4;
        g.fill(x, mid - quarterH, x + DIVIDER_WIDTH, mid + quarterH, 0x50000000 | classColor);
        g.fill(x, top, x + DIVIDER_WIDTH, mid - quarterH, 0x18000000 | classColor);
        g.fill(x, mid + quarterH, x + DIVIDER_WIDTH, bottom, 0x18000000 | classColor);
    }

    private void renderHorizontalSeparator(GuiGraphics g, int x, int y, int width, int classColor) {
        int mid = x + width / 2;
        int core = width / 6;
        g.fill(mid - core, y, mid + core, y + 1, 0x50000000 | classColor);
        g.fill(x, y, mid - core, y + 1, 0x18000000 | classColor);
        g.fill(mid + core, y, x + width, y + 1, 0x18000000 | classColor);
    }

    private void renderSectionLabel(GuiGraphics g, String titleKey, int x, int y, int width) {
        Component title = Component.translatable(titleKey).withStyle(Style.EMPTY.withColor(0x909090));
        g.drawString(font, title, x, y, 0x909090, false);
        int tw = font.width(title);
        g.fill(x + tw + 6, y + font.lineHeight / 2, x + width, y + font.lineHeight / 2 + 1, 0x1CFFFFFF);
    }

    private int renderExtraAbilitySection(GuiGraphics g, int x, int y, int contentWidth,
                                           PlayerClass.ExtraAbilitySection section, int classColor, int playerLevel) {
        boolean locked = section.unlockLevel() > 0 && playerLevel < section.unlockLevel();

        // Section title centered with decorative lines
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
            // Locked: darkened overlay with lock text
            int cardTop = y;
            int entryH = section.entries().size() * (font.lineHeight + 4) + CARD_PAD * 2;
            int cardBottom = cardTop + entryH;
            g.fill(x, cardTop, x + contentWidth - 8, cardBottom, 0x30000000);

            // Lock text centered
            Component lockMsg = Component.translatable("gui.archetype.locked", section.unlockLevel());
            int lockMsgW = font.width(lockMsg);
            int lockX = centerX - lockMsgW / 2;
            int lockY = cardTop + entryH / 2 - font.lineHeight / 2;
            g.drawString(font, lockMsg, lockX, lockY, 0x555555, false);

            y = cardBottom + 4;
        } else {
            // Unlocked: show entries
            for (PlayerClass.ExtraAbilityEntry entry : section.entries()) {
                Component entryName = Component.translatable(entry.nameKey());
                Component entryDesc = Component.translatable(entry.descriptionKey());

                // Entry card
                List<FormattedCharSequence> descLines = font.split(entryDesc, contentWidth - 28);
                int totalH = font.lineHeight + descLines.size() * (font.lineHeight + 1) + 4;
                g.fill(x + 4, y - 2, x + contentWidth - 12, y + totalH, 0x10000000 | classColor);
                g.fill(x + 4, y - 2, x + 6, y + totalH, 0x40000000 | classColor);

                g.drawString(font, entryName, x + 10, y, 0xDDDDDD, false);
                y += font.lineHeight + 1;

                for (FormattedCharSequence line : descLines) {
                    g.drawString(font, line, x + 10, y, 0x888888, false);
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
        int thumbY = (int) top + (int) ((viewH - thumbH) * (scrollOffset / maxScroll));
        g.fill(x, (int) top, x + SCROLLBAR_WIDTH, (int) (top + viewH), 0x10FFFFFF);
        g.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, 0x80000000 | classColor);
    }

    private void renderLevelTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ClientClassData data = ClientClassData.getInstance();
        int level = data.getLevel();
        int xp = data.getExperience();
        int neededXp = PlayerClassData.experienceForLevel(level + 1, 100);
        int maxLevel = 20;

        List<Component> lines = new ArrayList<>();

        // Header: Level X / 20
        lines.add(Component.translatable("gui.archetype.level", level)
                .append(Component.literal(" / " + maxLevel).withStyle(Style.EMPTY.withColor(0x666666))));

        // XP progress
        if (level < maxLevel) {
            int pct = neededXp > 0 ? (int) ((float) xp / neededXp * 100) : 0;
            lines.add(Component.literal(xp + " / " + neededXp + " XP (" + pct + "%)")
                    .withStyle(Style.EMPTY.withColor(0x888888)));
        } else {
            lines.add(Component.translatable("gui.archetype.max_level")
                    .withStyle(Style.EMPTY.withColor(0xFFCC88)));
        }

        // Ability unlock levels
        for (PlayerClass.ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
            int unlock = ability.unlockLevel();
            if (unlock > 1) {
                Component name = Component.translatable(ability.nameKey());
                if (unlock > level) {
                    lines.add(Component.translatable("gui.archetype.level_unlock", unlock)
                            .withStyle(Style.EMPTY.withColor(0xCC4444))
                            .append(Component.literal(" "))
                            .append(name.copy().withStyle(Style.EMPTY.withColor(0xAAAAAA))));
                } else {
                    lines.add(Component.literal("\u2714 ").withStyle(Style.EMPTY.withColor(0x44CC44))
                            .append(name.copy().withStyle(Style.EMPTY.withColor(0x44CC44))));
                }
            }
        }

        // Progression: show only the next milestone
        List<PlayerClass.LevelMilestone> milestones = playerClass.getProgression();
        if (!milestones.isEmpty()) {
            PlayerClass.LevelMilestone nextMilestone = null;
            for (PlayerClass.LevelMilestone milestone : milestones) {
                if (milestone.level() > level) {
                    nextMilestone = milestone;
                    break;
                }
            }
            if (nextMilestone != null) {
                lines.add(Component.empty());
                lines.add(Component.translatable("gui.archetype.next_level")
                        .withStyle(Style.EMPTY.withColor(0xFFCC88)));
                Component desc = Component.translatable(nextMilestone.descriptionKey());
                lines.add(Component.translatable("gui.archetype.level_unlock", nextMilestone.level())
                        .withStyle(Style.EMPTY.withColor(0x888888))
                        .append(Component.literal(" "))
                        .append(desc.copy().withStyle(Style.EMPTY.withColor(0xAAAAAA))));
            }
        }

        // Compute tooltip dimensions
        int maxW = 0;
        for (Component line : lines) {
            maxW = Math.max(maxW, font.width(line));
        }
        int tipW = maxW + 12;
        int lineH = font.lineHeight + 2;
        int tipH = 8;
        for (Component line : lines) {
            tipH += line.getString().isEmpty() ? 4 : lineH;
        }

        int tipX = mouseX + 12;
        int tipY = mouseY - 8;

        // Keep on screen
        if (tipX + tipW > width - 4) tipX = mouseX - tipW - 4;
        if (tipY + tipH > height - 4) tipY = height - tipH - 4;
        if (tipY < 4) tipY = 4;

        // Background
        g.fill(tipX, tipY, tipX + tipW, tipY + tipH, 0xFF0E0E18);
        // Border
        int classColor = playerClass.getColor();
        int bc = 0xCC000000 | classColor;
        g.fill(tipX, tipY, tipX + tipW, tipY + 1, bc);
        g.fill(tipX, tipY + tipH - 1, tipX + tipW, tipY + tipH, bc);
        g.fill(tipX, tipY, tipX + 1, tipY + tipH, bc);
        g.fill(tipX + tipW - 1, tipY, tipX + tipW, tipY + tipH, bc);

        // Text
        int textY = tipY + 4;
        for (Component line : lines) {
            if (line.getString().isEmpty()) {
                textY += 4;
                continue;
            }
            g.drawString(font, line, tipX + 6, textY, 0xFFFFFF, false);
            textY += lineH;
        }
    }

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
}
