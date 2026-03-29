package com.mod.archetype.gui;

import com.mod.archetype.config.ConfigManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class AbilityHudOverlay {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 4;
    private static final int RESOURCE_BAR_WIDTH = 60;
    private static final int RESOURCE_BAR_HEIGHT = 8;

    // Animation state
    private static final Map<ResourceLocation, Long> readyFlashTimestamps = new HashMap<>();
    private static final Map<ResourceLocation, Integer> previousCooldowns = new HashMap<>();
    private static float smoothResource = -1f;

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
            renderResourceBar(graphics, resX, startY + (SLOT_SIZE - RESOURCE_BAR_HEIGHT) / 2, data, playerClass, partialTick);
        }

        if (hudScale != 1.0f) {
            pose.popPose();
        }
    }

    private static void renderAbilitySlot(GuiGraphics g, int x, int y, int slotIndex,
                                          ClientClassData data, PlayerClass playerClass, float partialTick) {
        Font font = Minecraft.getInstance().font;
        long now = System.currentTimeMillis();

        if (playerClass == null || slotIndex >= playerClass.getActiveAbilities().size()) {
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
            return;
        }

        var ability = playerClass.getActiveAbilities().get(slotIndex);
        ResourceLocation abilityId = new ResourceLocation(ability.type().getNamespace(), ability.slot());

        // Determine ability state
        boolean locked = ability.unlockLevel() > data.getLevel();
        var chargeInfo = data.getCharge(abilityId);
        var cooldownInfo = data.getCooldown(abilityId);
        int remaining = cooldownInfo != null ? cooldownInfo.remaining() : 0;
        boolean onCooldown = remaining > 0 || (chargeInfo != null && chargeInfo.current() == 0);
        boolean ready = !locked && !onCooldown;

        // Track cooldown transitions for "ready" flash
        int prevCd = previousCooldowns.getOrDefault(abilityId, 0);
        if (prevCd > 0 && remaining == 0 && (chargeInfo == null || chargeInfo.current() > 0)) {
            readyFlashTimestamps.put(abilityId, now);
        }
        previousCooldowns.put(abilityId, remaining);

        // --- Item (rendered first — below all overlays) ---
        String itemId = ability.item();

        // Morph form_shift: switch to dragon_breath when form is active
        boolean isMorphFormShift = ability.type().getPath().equals("form_shift");
        boolean isFormActive = isMorphFormShift && data.isToggleActive(abilityId);
        if (isMorphFormShift && isFormActive) {
            itemId = "minecraft:dragon_breath";
        }

        if (itemId != null && !itemId.isEmpty()) {
            try {
                var itemRL = new ResourceLocation(itemId);
                var item = BuiltInRegistries.ITEM.get(itemRL);
                if (item != null && item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    g.renderItem(stack, x + 2, y + 2);
                }
            } catch (Exception e) {
            }
        }

        // All remaining 2D elements rendered at z+200 to appear above the item
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);

        // --- Slot background ---
        if (locked) {
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40000000);
        } else if (ready) {
            g.fillGradient(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40101010, 0x60181818);
        } else {
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x50000000);
        }

        // --- Border ---
        int borderColor;
        if (locked) {
            borderColor = 0x30FFFFFF;
        } else if (ready) {
            // Gentle breathing pulse on the border when ready
            float breathe = (float) (Math.sin(now * 0.003) * 0.15 + 0.55);
            int alpha = (int) (breathe * 255);
            borderColor = (alpha << 24) | 0xBBBBBB;
        } else {
            // Dim border when on cooldown
            borderColor = 0x25FFFFFF;
        }

        // "Ready" flash — bright border flash when ability comes off cooldown
        Long flashTime = readyFlashTimestamps.get(abilityId);
        if (flashTime != null) {
            float elapsed = (now - flashTime) / 1000f;
            if (elapsed < 0.6f) {
                float flash = 1.0f - (elapsed / 0.6f);
                flash = flash * flash; // ease-out
                int flashAlpha = (int) (flash * 200);
                borderColor = (Math.min(255, flashAlpha + ((borderColor >> 24) & 0xFF)) << 24) | 0xFFFFFF;
            } else {
                readyFlashTimestamps.remove(abilityId);
            }
        }

        g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, borderColor);

        // --- Key label ---
        int keyIdx = switch (ability.slot()) {
            case "ability_1" -> 0;
            case "ability_2" -> 1;
            case "ability_3" -> 2;
            default -> -1;
        };
        if (keyIdx >= 0) {
            int keyColor = locked ? 0x666666 : (onCooldown ? 0x999999 : 0xDDDDDD);
            String keyText = ArchetypeKeybinds.getSlotKeyDisplayShort(keyIdx);
            g.drawString(font, keyText, x + 2, y + 1, 0xFF000000, false);
            g.drawString(font, keyText, x + 2, y + 1, keyColor, true);
        }

        // --- Locked state ---
        if (locked) {
            String lockText = "\uD83D\uDD12";
            int lockX = x + SLOT_SIZE / 2 - font.width(lockText) / 2;
            g.drawString(font, lockText, lockX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
            g.drawString(font, lockText, lockX, y + SLOT_SIZE / 2 - 4, 0x888888, true);
            g.pose().popPose();
            return;
        }

        // --- Charge-based ability ---
        if (chargeInfo != null) {
            if (chargeInfo.current() == 0) {
                // No charges — animated diagonal sweep overlay
                float sweep = (float) ((now % 2000) / 2000.0);
                int overlayAlpha = (int) (0x60 + Math.sin(sweep * Math.PI * 2) * 0x1A);
                g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, (overlayAlpha << 24));
                String zeroText = "0";
                int zeroX = x + SLOT_SIZE / 2 - font.width(zeroText) / 2;
                g.drawString(font, zeroText, zeroX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
                g.drawString(font, zeroText, zeroX, y + SLOT_SIZE / 2 - 4, 0xFF5555, true);
            } else {
                // Charge count in bottom-right with green intensity based on charge ratio
                float ratio = (float) chargeInfo.current() / chargeInfo.max();
                int green = (int) (0x88 + ratio * 0x77);
                String chargeText = String.valueOf(chargeInfo.current());
                int textWidth = font.width(chargeText);
                int textY = y + SLOT_SIZE - 9;
                g.drawString(font, chargeText, x + SLOT_SIZE - textWidth - 1, textY, 0xFF000000, false);
                g.drawString(font, chargeText, x + SLOT_SIZE - textWidth - 1, textY, 0xFF000000 | (green << 8), true);
            }
            g.pose().popPose();
            return;
        }

        // --- Standard cooldown overlay ---
        if (remaining > 0) {
            int maxTicks = cooldownInfo.maxTicks();
            float smoothRemaining = remaining - partialTick;
            float progress = maxTicks > 0
                    ? Mth.clamp(smoothRemaining / maxTicks, 0f, 1f)
                    : 0f;
            int overlayHeight = Math.min(SLOT_SIZE, (int) (SLOT_SIZE * progress));

            // Cooldown overlay with gradient — darker at the bottom (remaining), lighter at the edge
            int overlayTop = y + SLOT_SIZE - overlayHeight;
            if (overlayHeight > 0) {
                g.fillGradient(x, overlayTop, x + SLOT_SIZE, y + SLOT_SIZE, 0x50000000, 0xA0000000);
            }

            // Cooldown seconds text — color shifts from red (long) → white (short)
            float cdSeconds = smoothRemaining / 20f;
            String cdText = String.valueOf((int) Math.ceil(cdSeconds));
            int textColor;
            if (cdSeconds > 5f) {
                textColor = 0xFF6666; // red-ish for long cooldowns
            } else if (cdSeconds > 2f) {
                // Interpolate red → white
                float t = (cdSeconds - 2f) / 3f;
                int r = 0xFF;
                int green = (int) (0xFF * (1f - t * 0.6f));
                int b = (int) (0xFF * (1f - t * 0.6f));
                textColor = (r << 16) | (green << 8) | b;
            } else {
                textColor = 0xFFFFFF; // white for almost ready
            }
            int cdTextX = x + SLOT_SIZE / 2 - font.width(cdText) / 2;
            g.drawString(font, cdText, cdTextX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
            g.drawString(font, cdText, cdTextX, y + SLOT_SIZE / 2 - 4, textColor, true);

            // Thin progress line at the bottom of the slot
            int lineWidth = (int) ((1f - progress) * SLOT_SIZE);
            if (lineWidth > 0) {
                g.fill(x, y + SLOT_SIZE - 1, x + lineWidth, y + SLOT_SIZE, 0xCC55FF55);
            }
        }

        g.pose().popPose();
    }

    private static void renderResourceBar(GuiGraphics g, int x, int y,
                                          ClientClassData data, PlayerClass playerClass, float partialTick) {
        Font font = Minecraft.getInstance().font;
        var res = playerClass.getResource();
        float current = data.getResourceCurrent();
        float max = res.maxValue();

        // Smooth resource interpolation
        if (smoothResource < 0) {
            smoothResource = current;
        } else {
            float lerpSpeed = 0.15f;
            smoothResource += (current - smoothResource) * lerpSpeed;
            // Snap when close enough
            if (Math.abs(smoothResource - current) < 0.1f) {
                smoothResource = current;
            }
        }

        float progress = max > 0 ? Mth.clamp(smoothResource / max, 0f, 1f) : 0f;

        // Background with subtle gradient
        g.fillGradient(x, y, x + RESOURCE_BAR_WIDTH, y + RESOURCE_BAR_HEIGHT, 0x50000000, 0x60101010);

        // Fill
        int fillWidth = (int) (RESOURCE_BAR_WIDTH * progress);
        int baseColor = 0xFF000000 | res.color();

        // Compute a slightly lighter version for gradient top
        int r = (baseColor >> 16) & 0xFF;
        int green = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int lighterR = Math.min(255, r + 40);
        int lighterG = Math.min(255, green + 40);
        int lighterB = Math.min(255, b + 40);
        int lighterColor = 0xFF000000 | (lighterR << 16) | (lighterG << 8) | lighterB;

        int fillColor = baseColor;
        int fillColorTop = lighterColor;

        // Pulse when low
        if (progress < 0.2f) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.008) * 0.35 + 0.65);
            fillColor = applyBrightness(baseColor, pulse);
            fillColorTop = applyBrightness(lighterColor, pulse);
        }

        if (fillWidth > 0) {
            g.fillGradient(x, y, x + fillWidth, y + RESOURCE_BAR_HEIGHT, fillColorTop, fillColor);
        }

        // Thin highlight line at the top of the fill
        if (fillWidth > 1) {
            int highlightAlpha = (int) (progress * 0x40);
            g.fill(x, y, x + fillWidth, y + 1, (highlightAlpha << 24) | 0xFFFFFF);
        }

        // Border
        g.renderOutline(x, y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT, 0x30FFFFFF);

        // Text
        String text = String.format("%.0f/%.0f", current, max);
        int textColor = progress < 0.2f ? 0xFF8888 : 0xCCCCCC;
        g.drawString(font, text, x + RESOURCE_BAR_WIDTH + 4, y, textColor, true);
    }

    private static int applyBrightness(int color, float brightness) {
        int r = (int) (((color >> 16) & 0xFF) * brightness);
        int green = (int) (((color >> 8) & 0xFF) * brightness);
        int b = (int) ((color & 0xFF) * brightness);
        return 0xFF000000 | (r << 16) | (green << 8) | b;
    }
}
