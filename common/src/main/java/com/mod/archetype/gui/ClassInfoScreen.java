package com.mod.archetype.gui;

import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.core.PlayerClass.AttributeModifierEntry;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ClassInfoScreen extends Screen {

    private PlayerClass playerClass;
    private float scrollOffset = 0;
    private float contentHeight = 0;

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
        renderBackground(g);

        if (playerClass == null) {
            g.drawCenteredString(font, Component.translatable("gui.archetype.no_class"), width / 2, height / 2, 0xFF5555);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        ClientClassData data = ClientClassData.getInstance();
        int contentX = width / 6;
        int contentWidth = width * 2 / 3;
        int viewTop = 10;
        int viewBottom = height - 10;

        g.enableScissor(contentX, viewTop, contentX + contentWidth, viewBottom);

        int y = viewTop - (int) scrollOffset;

        // Header
        var pose = g.pose();
        pose.pushPose();
        pose.translate(contentX + contentWidth / 2f, y, 0);
        pose.scale(1.5f, 1.5f, 1);
        g.drawCenteredString(font, Component.translatable(playerClass.getNameKey()), 0, 0, 0xFF000000 | playerClass.getColor());
        pose.popPose();
        y += 22;

        // Level + XP bar
        int level = data.getLevel();
        int xp = data.getExperience();
        int neededXp = PlayerClassData.experienceForLevel(level + 1, 100);
        float xpProgress = neededXp > 0 ? (float) xp / neededXp : 0;

        g.drawString(font, Component.translatable("gui.archetype.level", level), contentX, y, 0xFFCC88, false);
        y += 12;
        ClassScreenRenderer.renderProgressBar(g, contentX, y, Math.min(200, contentWidth), 6, xpProgress, 0x44CC44);
        g.drawString(font, xp + "/" + neededXp + " XP", contentX + Math.min(200, contentWidth) + 4, y, 0x999999, false);
        y += 14;

        // Resource
        if (playerClass.getResource() != null) {
            var res = playerClass.getResource();
            float current = data.getResourceCurrent();
            float max = res.maxValue();
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.resource", contentX, y, contentWidth);
            y += 16;
            ClassScreenRenderer.renderProgressBar(g, contentX, y, Math.min(200, contentWidth), 8, current / max, res.color());
            g.drawString(font, String.format("%.0f/%.0f", current, max), contentX + Math.min(200, contentWidth) + 4, y, 0xFF000000 | res.color(), false);
            y += 16;
        }

        // Attributes
        if (!playerClass.getAttributes().isEmpty()) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.attributes", contentX, y, contentWidth);
            y += 16;
            int barWidth = Math.min(120, contentWidth / 3);
            for (AttributeModifierEntry attr : playerClass.getAttributes()) {
                String attrName = attr.attribute().getPath().replace("generic.", "").replace("_", " ");
                attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
                double baseValue = getBaseValue(attr.attribute().toString());
                ClassScreenRenderer.renderAttributeBar(g, font, attrName, baseValue + attr.value(), baseValue, contentX, y, barWidth);
                y += 14;
            }
            y += 10;
        }

        // Active abilities
        if (!playerClass.getActiveAbilities().isEmpty()) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.abilities", contentX, y, contentWidth);
            y += 16;
            String[] slotKeys = {"R", "V", "G"};
            for (ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
                int slotIdx = switch (ability.slot()) {
                    case "ability_1" -> 0;
                    case "ability_2" -> 1;
                    case "ability_3" -> 2;
                    default -> 0;
                };
                g.drawString(font, "[" + (slotIdx < slotKeys.length ? slotKeys[slotIdx] : "?") + "]", contentX, y, 0xFFCC88, false);
                g.drawString(font, Component.translatable(ability.nameKey()), contentX + 30, y, 0xFFFFFF, false);
                String cdText = (ability.cooldownTicks() / 20) + "s";
                g.drawString(font, cdText, contentX + contentWidth - font.width(cdText), y, 0x888888, false);
                y += 12;
                g.drawString(font, Component.translatable(ability.descriptionKey()), contentX + 30, y, 0x999999, false);
                y += 14;
            }
            y += 10;
        }

        // Passives
        if (!playerClass.getPassiveAbilities().isEmpty()) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.passives", contentX, y, contentWidth);
            y += 16;
            var sorted = playerClass.getPassiveAbilities().stream()
                    .sorted((a, b) -> Boolean.compare(b.positive(), a.positive()))
                    .toList();
            for (PassiveAbilityEntry passive : sorted) {
                String marker = passive.positive() ? "\u2714" : "\u2718";
                int color = passive.positive() ? 0xFF44CC44 : 0xFFCC4444;
                g.drawString(font, marker, contentX, y, color, false);
                g.drawString(font, Component.translatable(passive.nameKey()), contentX + 14, y, color, false);
                y += 12;
                g.drawString(font, Component.translatable(passive.descriptionKey()), contentX + 14, y, 0x999999, false);
                y += 14;
            }
        }

        contentHeight = y + scrollOffset - viewTop + 20;
        g.disableScissor();

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= (float) (delta * 15);
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - (height - 20)));
        return true;
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
