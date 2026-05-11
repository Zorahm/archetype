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
import net.minecraft.resources.Identifier;
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
    private static final Map<Identifier, Long> readyFlashTimestamps = new HashMap<>();
    private static final Map<Identifier, Integer> previousCooldowns = new HashMap<>();
    private static final Map<Identifier, Long> cooldownSpinnerStartTimes = new HashMap<>();
    private static float smoothResource = -1f;

    public static void render(GuiGraphics graphics, float partialTick) {
        ClientClassData data = ClientClassData.getInstance();
        if (!data.hasClass()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        if (mc.player == null || mc.player.isSpectator()) return;

        float deathAlpha = 1.0f;
        if (mc.player.isDeadOrDying()) {
            deathAlpha = 0.3f;
        }

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
            pose.pushMatrix();
            pose.scale(hudScale, hudScale);
            startX = (int) (startX / hudScale);
            startY = (int) (startY / hudScale);
        }

        // Render only slots used by the class
        for (int i = 0; i < slotCount; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);
            renderAbilitySlot(graphics, slotX, startY, i, data, playerClass, partialTick, deathAlpha);
        }

        // Resource bar
        if (playerClass.getResource() != null && ConfigManager.client().showResourceBar) {
            int resX = startX + slotCount * (SLOT_SIZE + SLOT_SPACING) + 4;
            renderResourceBar(graphics, resX, startY + (SLOT_SIZE - RESOURCE_BAR_HEIGHT) / 2, data, playerClass, partialTick);
        }

        if (hudScale != 1.0f) {
            pose.popMatrix();
        }
    }

    private static void renderAbilitySlot(GuiGraphics g, int x, int y, int slotIndex,
                                           ClientClassData data, PlayerClass playerClass, float partialTick,
                                           float deathAlpha) {
        Font font = Minecraft.getInstance().font;
        long now = System.currentTimeMillis();

        if (playerClass == null || slotIndex >= playerClass.getActiveAbilities().size()) {
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
            return;
        }

        var ability = playerClass.getActiveAbilities().get(slotIndex);
        Identifier abilityId = Identifier.fromNamespaceAndPath(ability.type().getNamespace(), ability.slot());

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

        // Spinner timing: keep a stable start time for the whole cooldown
        if (remaining > 0) {
            if (!cooldownSpinnerStartTimes.containsKey(abilityId)) {
                int maxTicks = cooldownInfo != null ? cooldownInfo.maxTicks() : 20;
                long inferredStart = now - Math.max(0L, (long) (maxTicks - remaining)) * 50L;
                cooldownSpinnerStartTimes.put(abilityId, inferredStart);
            }
        } else {
            cooldownSpinnerStartTimes.remove(abilityId);
        }

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
                var itemRL = Identifier.parse(itemId);
                var item = BuiltInRegistries.ITEM.getValue(itemRL);
                if (item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    g.renderItem(stack, x + 2, y + 2);
                    if (deathAlpha < 1.0f) {
                        int alpha = (int) (deathAlpha * 255);
                        g.fill(x + 2, y + 2, x + 18, y + 18, (alpha << 24) | 0x000000);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // --- Slot background ---
        // Затемнение только на перезарядке (ниже, в блоке cooldown).
        if (locked) {
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40000000);
        }

        // --- Border / spinner ---
        if (locked) {
            g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xAA000000);
        } else if (ready) {
            // Static border when ready
            g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xCC222222);
            readyFlashTimestamps.remove(abilityId);
        } else {
            // Spinner animation on cooldown border
            if (remaining > 0) {
                int maxTicks = cooldownInfo != null ? cooldownInfo.maxTicks() : 20;
                renderCooldownSpinner(g, x, y, SLOT_SIZE, abilityId, remaining, maxTicks, partialTick, now);
            } else {
                g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0x25FFFFFF);
            }
        }

        // --- Key label ---
        int keyIdx = switch (ability.slot()) {
            case "ability_1" -> 0;
            case "ability_2" -> 1;
            case "ability_3" -> 2;
            default -> -1;
        };
        if (keyIdx >= 0) {
            int keyColor = locked ? 0xFF666666 : (onCooldown ? 0xFF999999 : 0xFFDDDDDD);
            String keyText = ArchetypeKeybinds.getSlotKeyDisplayShort(keyIdx);
            g.drawString(font, keyText, x + 2, y + 1, 0xFF000000, false);
            g.drawString(font, keyText, x + 2, y + 1, keyColor, true);
        }

        // --- Locked state ---
        if (locked) {
            String lockText = "\uD83D\uDD12";
            int lockX = x + SLOT_SIZE / 2 - font.width(lockText) / 2;
            g.drawString(font, lockText, lockX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
            g.drawString(font, lockText, lockX, y + SLOT_SIZE / 2 - 4, 0xFF888888, true);
            return;
        }

        // --- Charge-based ability ---
        if (chargeInfo != null) {
            if (chargeInfo.current() == 0) {
                // No charges — static overlay
                g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x60000000);

                if (remaining > 0) {
                    // Show refill countdown in seconds
                    float cdSeconds = remaining / 20f;
                    String cdText = String.valueOf((int) Math.ceil(cdSeconds));
                    int textColor;
                    if (cdSeconds > 5f) {
                        textColor = 0xFFFF6666;
                    } else if (cdSeconds > 2f) {
                        float t = (cdSeconds - 2f) / 3f;
                        textColor = 0xFF000000 | (0xFF << 16) | ((int) (0xFF * (1f - t * 0.6f)) << 8) | (int) (0xFF * (1f - t * 0.6f));
                    } else {
                        textColor = 0xFFFFFFFF;
                    }
                    int cdTextX = x + SLOT_SIZE / 2 - font.width(cdText) / 2;
                    g.drawString(font, cdText, cdTextX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
                    g.drawString(font, cdText, cdTextX, y + SLOT_SIZE / 2 - 4, textColor, true);
                } else {
                    String zeroText = "0";
                    int zeroX = x + SLOT_SIZE / 2 - font.width(zeroText) / 2;
                    g.drawString(font, zeroText, zeroX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
                    g.drawString(font, zeroText, zeroX, y + SLOT_SIZE / 2 - 4, 0xFFFF5555, true);
                }
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
            return;
        }

        // --- Standard cooldown overlay ---
        if (remaining > 0) {
            // Static dim overlay during cooldown
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);

            // Cooldown seconds text — use server value to avoid showing "0" prematurely
            float cdSeconds = (remaining - partialTick) / 20f;
            String cdText = String.valueOf((int) Math.ceil(cdSeconds));
            int textColor;
            if (cdSeconds > 5f) {
                textColor = 0xFFFF6666;
            } else if (cdSeconds > 2f) {
                float t = (cdSeconds - 2f) / 3f;
                int r = 0xFF;
                int green = (int) (0xFF * (1f - t * 0.6f));
                int b = (int) (0xFF * (1f - t * 0.6f));
                textColor = 0xFF000000 | (r << 16) | (green << 8) | b;
            } else {
                textColor = 0xFFFFFFFF;
            }
            int cdTextX = x + SLOT_SIZE / 2 - font.width(cdText) / 2;
            g.drawString(font, cdText, cdTextX, y + SLOT_SIZE / 2 - 4, 0xFF000000, false);
            g.drawString(font, cdText, cdTextX, y + SLOT_SIZE / 2 - 4, textColor, true);
        }
    }

    private static void renderCooldownSpinner(GuiGraphics g, int x, int y, int size,
                                              Identifier abilityId, int remaining, int maxTicks,
                                              float partialTick, long now) {
        // Base border
        g.renderOutline(x, y, size, size, 0xAA000000);

        float progress = maxTicks > 0
                ? Mth.clamp((remaining - partialTick) / maxTicks, 0f, 1f)
                : 0f;

        Long startTime = cooldownSpinnerStartTimes.get(abilityId);
        if (startTime == null) {
            startTime = now;
            cooldownSpinnerStartTimes.put(abilityId, startTime);
        }

        int edge = size - 1;
        float perimeter = edge * 4f;

        // Stable continuous rotation based on cooldown start time.
        // This does not reset when `remaining` changes.
        float elapsedSeconds = (now - startTime + (long) (partialTick * 50f)) / 1000f;
        float speed = 1.6f; // rotations per second over the perimeter
        float phaseOffset = getStablePhaseOffset(abilityId, perimeter);
        float head = (elapsedSeconds * speed * perimeter + phaseOffset) % perimeter;

        // Trail length grows slightly while cooldown is longer
        int trailSegments = 13;
        int segmentLen = 3;
        float spacing = 3.2f;

        for (int i = 0; i < trailSegments; i++) {
            float p = head - i * spacing;
            while (p < 0f) p += perimeter;
            p %= perimeter;

            float fade = 1f - (i / (float) trailSegments);
            float eased = fade * fade;
            int alpha = (int) (255 * eased);
            int segColor = (alpha << 24) | 0x88FF88;

            drawPerimeterChunk(g, x, y, size, p, segmentLen, segColor);
        }

        // Subtle extra glow for very long cooldowns
        if (progress > 0.65f) {
            int glowAlpha = (int) (18 * progress);
            g.renderOutline(x + 1, y + 1, size - 2, size - 2, (glowAlpha << 24) | 0x88FF88);
        }
    }

    private static float getStablePhaseOffset(Identifier abilityId, float perimeter) {
        int hash = abilityId.hashCode();
        hash ^= (hash >>> 16);
        int positive = hash & 0x7fffffff;
        float normalized = positive / (float) Integer.MAX_VALUE;
        return normalized * perimeter;
    }

    private static void drawPerimeterChunk(GuiGraphics g, int x, int y, int size,
                                           float pos, int len, int color) {
        int edge = size - 1;

        // Top edge: left to right
        if (pos < edge) {
            int sx = x + (int) pos;
            int ex = Math.min(x + size, sx + len);
            g.fill(sx, y, ex, y + 1, color);
            return;
        }

        pos -= edge;

        // Right edge: top to bottom
        if (pos < edge) {
            int sy = y + (int) pos;
            int ey = Math.min(y + size, sy + len);
            g.fill(x + size - 1, sy, x + size, ey, color);
            return;
        }

        pos -= edge;

        // Bottom edge: right to left
        if (pos < edge) {
            int ex = x + size - 1 - (int) pos;
            int sx = Math.max(x, ex - len + 1);
            g.fill(sx, y + size - 1, ex + 1, y + size, color);
            return;
        }

        pos -= edge;

        // Left edge: bottom to top
        if (pos < 0f || pos >= edge) return; // guard against float drift at boundaries
        int ey = y + size - 1 - (int) pos;
        int sy = Math.max(y, ey - len + 1);
        if (sy >= y && ey < y + size) {
            g.fill(x, sy, x + 1, ey + 1, color);
        }
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
        int textColor = progress < 0.2f ? 0xFFFF8888 : 0xFFCCCCCC;
        g.drawString(font, text, x + RESOURCE_BAR_WIDTH + 4, y, textColor, true);
    }

    private static int applyBrightness(int color, float brightness) {
        int r = (int) (((color >> 16) & 0xFF) * brightness);
        int green = (int) (((color >> 8) & 0xFF) * brightness);
        int b = (int) ((color & 0xFF) * brightness);
        return 0xFF000000 | (r << 16) | (green << 8) | b;
    }
}