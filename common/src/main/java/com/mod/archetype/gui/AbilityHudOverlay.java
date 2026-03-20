package com.mod.archetype.gui;

import com.mod.archetype.config.ConfigManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AbilityHudOverlay {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 4;
    private static final int RESOURCE_BAR_WIDTH = 60;
    private static final int RESOURCE_BAR_HEIGHT = 8;

    public static void render(GuiGraphics graphics, float partialTick) {
        ClientClassData data = ClientClassData.getInstance();
        if (!data.hasClass()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        if (mc.player == null || mc.player.isDeadOrDying() || mc.player.isSpectator()) return;

        PlayerClass playerClass = data.getClassId() != null
                ? ClassRegistry.getInstance().get(data.getClassId()).orElse(null)
                : null;

        float hudScale = ConfigManager.client().hudScale;
        String position = ConfigManager.client().abilityBarPosition;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int slotCount = playerClass != null ? playerClass.getActiveAbilities().size() : 0;
        if (slotCount == 0) return;

        int totalWidth = slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING;
        if (playerClass.getResource() != null && ConfigManager.client().showResourceBar) {
            totalWidth += 8 + RESOURCE_BAR_WIDTH + 40;
        }

        int startX, startY;
        switch (position) {
            case "hotbar_left" -> {
                startX = screenWidth / 2 - 91 - totalWidth - 10;
                startY = screenHeight - 22 - SLOT_SIZE / 2;
            }
            case "top_center" -> {
                startX = (screenWidth - totalWidth) / 2;
                startY = 5;
            }
            default -> { // hotbar_right
                startX = screenWidth / 2 + 91 + 10;
                startY = screenHeight - 22 - SLOT_SIZE / 2;
            }
        }

        var pose = graphics.pose();
        if (hudScale != 1.0f) {
            pose.pushPose();
            pose.scale(hudScale, hudScale, 1.0f);
            startX = (int) (startX / hudScale);
            startY = (int) (startY / hudScale);
        }

        // Render only slots used by the class
        for (int i = 0; i < slotCount; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);
            renderAbilitySlot(graphics, slotX, startY, i, data, playerClass, partialTick);
        }

        // Resource bar
        if (playerClass.getResource() != null && ConfigManager.client().showResourceBar) {
            int resX = startX + slotCount * (SLOT_SIZE + SLOT_SPACING) + 4;
            renderResourceBar(graphics, resX, startY + (SLOT_SIZE - RESOURCE_BAR_HEIGHT) / 2, data, playerClass);
        }

        if (hudScale != 1.0f) {
            pose.popPose();
        }
    }

    private static void renderAbilitySlot(GuiGraphics g, int x, int y, int slotIndex,
                                            ClientClassData data, PlayerClass playerClass, float partialTick) {
        Font font = Minecraft.getInstance().font;
        String[] slotKeys = {"R", "V", "G"};

        // Background
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
        g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0x40FFFFFF);

        if (playerClass == null || slotIndex >= playerClass.getActiveAbilities().size()) return;

        var ability = playerClass.getActiveAbilities().get(slotIndex);

        // Key label from actual slot assignment
        int keyIdx = switch (ability.slot()) {
            case "ability_1" -> 0;
            case "ability_2" -> 1;
            case "ability_3" -> 2;
            default -> -1;
        };
        if (keyIdx >= 0 && keyIdx < slotKeys.length) {
            g.drawString(font, slotKeys[keyIdx], x + 2, y + 1, 0xCCCCCC, true);
        }

        // Check level lock
        if (ability.unlockLevel() > data.getLevel()) {
            g.drawCenteredString(font, "\uD83D\uDD12", x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0x888888);
            return;
        }

        // Ability ID key matches server-side format: namespace:slot
        ResourceLocation abilityId = new ResourceLocation(ability.type().getNamespace(), ability.slot());
        var chargeInfo = data.getCharge(abilityId);

        if (chargeInfo != null) {
            // Charge-based ability: show charge count and recharge overlay when 0
            if (chargeInfo.current() == 0) {
                // No charges — show full cooldown overlay (like ender pearl recharge)
                g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
                g.drawCenteredString(font, "0", x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0xFF5555);
            } else {
                // Has charges — show charge count in bottom-right
                String chargeText = String.valueOf(chargeInfo.current());
                int textWidth = font.width(chargeText);
                g.drawString(font, chargeText, x + SLOT_SIZE - textWidth - 1, y + SLOT_SIZE - 9, 0x55FF55, true);
            }
        } else {
            // Standard cooldown overlay
            var cooldownInfo = data.getCooldown(abilityId);
            int remaining = cooldownInfo != null ? cooldownInfo.remaining() : 0;
            if (remaining > 0) {
                int maxTicks = cooldownInfo.maxTicks();
                float smoothRemaining = remaining - partialTick;
                float progress = maxTicks > 0
                        ? Math.min(1.0f, Math.max(0.0f, smoothRemaining / maxTicks))
                        : 0f;
                int overlayHeight = Math.min(SLOT_SIZE, (int) (SLOT_SIZE * progress));
                g.fill(x, y + SLOT_SIZE - overlayHeight, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
                String cdText = String.valueOf((int) Math.ceil(remaining / 20.0));
                g.drawCenteredString(font, cdText, x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0xFFFFFF);
            }
        }
    }

    private static void renderResourceBar(GuiGraphics g, int x, int y,
                                            ClientClassData data, PlayerClass playerClass) {
        Font font = Minecraft.getInstance().font;
        var res = playerClass.getResource();
        float current = data.getResourceCurrent();
        float max = res.maxValue();
        float progress = max > 0 ? current / max : 0;

        // Background
        g.fill(x, y, x + RESOURCE_BAR_WIDTH, y + RESOURCE_BAR_HEIGHT, 0x40000000);

        // Fill
        int fillWidth = (int) (RESOURCE_BAR_WIDTH * Math.max(0, Math.min(1, progress)));
        int color = 0xFF000000 | res.color();

        // Pulse when low
        if (progress < 0.2f) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.3 + 0.7);
            int r = (int) (((color >> 16) & 0xFF) * pulse);
            int green = (int) (((color >> 8) & 0xFF) * pulse);
            int b = (int) ((color & 0xFF) * pulse);
            color = 0xFF000000 | (r << 16) | (green << 8) | b;
        }

        g.fill(x, y, x + fillWidth, y + RESOURCE_BAR_HEIGHT, color);

        // Text
        String text = String.format("%.0f/%.0f", current, max);
        g.drawString(font, text, x + RESOURCE_BAR_WIDTH + 4, y, 0xCCCCCC, true);
    }
}
