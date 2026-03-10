package com.mod.archetype.gui;

import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.core.PlayerClass.AttributeModifierEntry;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.network.ClassSelectPacket;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ClassDetailScreen extends Screen {

    private final PlayerClass playerClass;
    private final int mode;
    private float scrollOffset = 0;
    private float contentHeight = 0;
    private boolean showConfirmation = false;
    private float barAnimProgress = 0;

    public ClassDetailScreen(PlayerClass playerClass, int mode) {
        super(Component.translatable(playerClass.getNameKey()));
        this.playerClass = playerClass;
        this.mode = mode;
    }

    @Override
    protected void init() {
        // Back button
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.archetype.back"),
                        btn -> Minecraft.getInstance().setScreen(new ClassSelectionScreen(mode)))
                .bounds(5, 5, 60, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        barAnimProgress = Math.min(1.0f, barAnimProgress + partialTick * 0.02f);

        int contentX = width / 6;
        int contentWidth = width * 2 / 3;
        int viewTop = 30;
        int viewBottom = height - 10;

        g.enableScissor(contentX, viewTop, contentX + contentWidth, viewBottom);

        int y = viewTop - (int) scrollOffset;

        // Header
        y = renderHeader(g, contentX, y, contentWidth);
        y += 15;

        // Attributes
        if (!playerClass.getAttributes().isEmpty()) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.attributes", contentX, y, contentWidth);
            y += 16;
            y = renderAttributes(g, contentX, y, contentWidth);
            y += 10;
        }

        // Active abilities
        if (!playerClass.getActiveAbilities().isEmpty()) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.abilities", contentX, y, contentWidth);
            y += 16;
            y = renderActiveAbilities(g, contentX, y, contentWidth);
            y += 10;
        }

        // Passive abilities
        if (!playerClass.getPassiveAbilities().isEmpty()) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.passives", contentX, y, contentWidth);
            y += 16;
            y = renderPassives(g, contentX, y, contentWidth);
            y += 10;
        }

        // Resource
        if (playerClass.getResource() != null) {
            ClassScreenRenderer.renderSectionHeader(g, font, "gui.archetype.resource", contentX, y, contentWidth);
            y += 16;
            y = renderResource(g, contentX, y, contentWidth);
            y += 10;
        }

        // Select button
        y = renderSelectButton(g, contentX, y, contentWidth, mouseX, mouseY);

        contentHeight = y + scrollOffset - viewTop + 20;

        g.disableScissor();

        super.render(g, mouseX, mouseY, partialTick);

        if (showConfirmation) {
            renderConfirmation(g, mouseX, mouseY);
        }
    }

    private int renderHeader(GuiGraphics g, int x, int y, int width) {
        // Class name (scaled)
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + width / 2f, y, 0);
        pose.scale(1.5f, 1.5f, 1);
        Component name = Component.translatable(playerClass.getNameKey());
        g.drawCenteredString(font, name, 0, 0, 0xFF000000 | playerClass.getColor());
        pose.popPose();
        y += 20;

        // Lore
        if (!playerClass.getLoreKeys().isEmpty()) {
            Component lore = Component.translatable(playerClass.getLoreKeys().get(0))
                    .withStyle(s -> s.withItalic(true).withColor(0xAAAAAA));
            g.drawCenteredString(font, lore, x + width / 2, y, 0xAAAAAA);
            y += 12;
        }
        return y;
    }

    private int renderAttributes(GuiGraphics g, int x, int y, int contentWidth) {
        int barWidth = Math.min(120, contentWidth / 3);
        for (AttributeModifierEntry attr : playerClass.getAttributes()) {
            String attrName = attr.attribute().getPath().replace("generic.", "")
                    .replace("_", " ");
            attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
            double baseValue = getBaseValue(attr.attribute().toString());
            double value = baseValue + attr.value() * barAnimProgress;
            ClassScreenRenderer.renderAttributeBar(g, font, attrName, value, baseValue, x, y, barWidth);
            y += 14;
        }
        return y;
    }

    private int renderActiveAbilities(GuiGraphics g, int x, int y, int contentWidth) {
        String[] slotKeys = {"R", "V", "G"};
        for (ActiveAbilityEntry ability : playerClass.getActiveAbilities()) {
            int slotIndex = switch (ability.slot()) {
                case "ability_1" -> 0;
                case "ability_2" -> 1;
                case "ability_3" -> 2;
                default -> 0;
            };
            String key = slotIndex < slotKeys.length ? slotKeys[slotIndex] : "?";

            // Key indicator
            g.drawString(font, "[" + key + "]", x, y, 0xFFCC88, false);

            // Name
            Component aName = Component.translatable(ability.nameKey());
            g.drawString(font, aName, x + 30, y, 0xFFFFFF, false);

            // Cooldown
            String cdText = (ability.cooldownTicks() / 20) + "s";
            g.drawString(font, cdText, x + contentWidth - font.width(cdText), y, 0x888888, false);
            y += 12;

            // Description
            Component desc = Component.translatable(ability.descriptionKey());
            g.drawString(font, desc, x + 30, y, 0x999999, false);
            y += 14;
        }
        return y;
    }

    private int renderPassives(GuiGraphics g, int x, int y, int contentWidth) {
        // Sort: positives first
        var sorted = playerClass.getPassiveAbilities().stream()
                .sorted((a, b) -> Boolean.compare(b.positive(), a.positive()))
                .toList();
        for (PassiveAbilityEntry passive : sorted) {
            String marker = passive.positive() ? "\u2714" : "\u2718";
            int color = passive.positive() ? 0xFF44CC44 : 0xFFCC4444;
            g.drawString(font, marker, x, y, color, false);
            Component pName = Component.translatable(passive.nameKey());
            g.drawString(font, pName, x + 14, y, color, false);
            y += 12;

            Component pDesc = Component.translatable(passive.descriptionKey());
            g.drawString(font, pDesc, x + 14, y, 0x999999, false);
            y += 14;
        }
        return y;
    }

    private int renderResource(GuiGraphics g, int x, int y, int contentWidth) {
        var res = playerClass.getResource();
        Component resName = Component.translatable(res.typeKey());
        g.drawString(font, resName, x, y, 0xFF000000 | res.color(), false);
        y += 12;
        ClassScreenRenderer.renderProgressBar(g, x, y, Math.min(200, contentWidth), 8, 1.0f, res.color());
        g.drawString(font, res.maxValue() + "/" + res.maxValue(), x + Math.min(200, contentWidth) + 4, y, 0xCCCCCC, false);
        y += 14;
        return y;
    }

    private int renderSelectButton(GuiGraphics g, int x, int y, int contentWidth, int mouseX, int mouseY) {
        int btnWidth = 160;
        int btnHeight = 24;
        int btnX = x + (contentWidth - btnWidth) / 2;
        int btnY = y + 10;

        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        int bgColor = hovered ? 0xC0446644 : 0xC0224422;
        g.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, bgColor);
        g.renderOutline(btnX, btnY, btnWidth, btnHeight, 0xFF44CC44);

        Component text = Component.translatable("gui.archetype.select_class");
        g.drawCenteredString(font, text, btnX + btnWidth / 2, btnY + (btnHeight - font.lineHeight) / 2, 0xFF44CC44);

        return btnY + btnHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showConfirmation) {
            // Yes button
            int yesX = width / 2 - 80;
            int yesY = height / 2 + 10;
            if (mouseX >= yesX && mouseX <= yesX + 60 && mouseY >= yesY && mouseY <= yesY + 20) {
                NetworkHandler.INSTANCE.sendToServer(new ClassSelectPacket(playerClass.getId(), mode == 1));
                onClose();
                return true;
            }
            // No button
            int noX = width / 2 + 20;
            if (mouseX >= noX && mouseX <= noX + 60 && mouseY >= yesY && mouseY <= yesY + 20) {
                showConfirmation = false;
                return true;
            }
            return true;
        }

        // Select button click detection
        int contentX = width / 6;
        int contentWidth = width * 2 / 3;
        int btnWidth = 160;
        int btnX = contentX + (contentWidth - btnWidth) / 2;
        // Approximate button Y (rough check)
        if (mouseX >= btnX && mouseX <= btnX + btnWidth && button == 0) {
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
        scrollOffset -= (float) (delta * 15);
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - (height - 40)));
        return true;
    }

    private void renderConfirmation(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, 0x80000000);
        Component message = Component.translatable("gui.archetype.confirm_rebirth");
        g.drawCenteredString(font, message, width / 2, height / 2 - 15, 0xFFFFFF);

        // Yes button
        int yesX = width / 2 - 80;
        int yesY = height / 2 + 10;
        boolean yesHover = mouseX >= yesX && mouseX <= yesX + 60 && mouseY >= yesY && mouseY <= yesY + 20;
        g.fill(yesX, yesY, yesX + 60, yesY + 20, yesHover ? 0xC0446644 : 0xC0333333);
        g.drawCenteredString(font, Component.translatable("gui.yes"), yesX + 30, yesY + 6, 0x44CC44);

        // No button
        int noX = width / 2 + 20;
        boolean noHover = mouseX >= noX && mouseX <= noX + 60 && mouseY >= yesY && mouseY <= yesY + 20;
        g.fill(noX, yesY, noX + 60, yesY + 20, noHover ? 0xC0664444 : 0xC0333333);
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
