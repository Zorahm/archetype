package com.mod.archetype.keybind;

import com.mod.archetype.gui.ClassInfoScreen;
import com.mod.archetype.network.AbilityReleasePacket;
import com.mod.archetype.network.AbilityUsePacket;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class ArchetypeKeybinds {

    public static final String ABILITY_1 = "key.archetype.ability_1";
    public static final String ABILITY_2 = "key.archetype.ability_2";
    public static final String ABILITY_3 = "key.archetype.ability_3";
    public static final String CLASS_INFO = "key.archetype.class_info";
    public static final String CATEGORY = "key.categories.archetype";

    public static final int DEFAULT_ABILITY_1 = GLFW.GLFW_KEY_R;
    public static final int DEFAULT_ABILITY_2 = GLFW.GLFW_KEY_V;
    public static final int DEFAULT_ABILITY_3 = GLFW.GLFW_KEY_G;
    public static final int DEFAULT_CLASS_INFO = GLFW.GLFW_KEY_J;

    private static String chargingSlot = null;
    private static int chargeTicks = 0;

    public static void tickKeybinds(KeyMapping ability1, KeyMapping ability2, KeyMapping ability3, KeyMapping classInfo) {
        ClientClassData data = ClientClassData.getInstance();
        if (!data.hasClass()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        if (ability1.consumeClick()) {
            sendAbilityUse("ability_1");
        }
        if (ability2.consumeClick()) {
            sendAbilityUse("ability_2");
        }
        if (ability3.consumeClick()) {
            sendAbilityUse("ability_3");
        }

        if (classInfo.consumeClick()) {
            mc.setScreen(new ClassInfoScreen());
        }

        // Handle charged ability release
        if (chargingSlot != null) {
            KeyMapping chargeKey = switch (chargingSlot) {
                case "ability_1" -> ability1;
                case "ability_2" -> ability2;
                case "ability_3" -> ability3;
                default -> null;
            };
            if (chargeKey != null && chargeKey.isDown()) {
                chargeTicks++;
            } else {
                NetworkHandler.INSTANCE.sendToServer(new AbilityReleasePacket(chargingSlot));
                chargingSlot = null;
                chargeTicks = 0;
            }
        }
    }

    private static void sendAbilityUse(String slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float dirX = 0, dirZ = 0;
        if (player != null) {
            float forward = player.input.forwardImpulse;
            float strafe = player.input.leftImpulse;
            if (forward != 0 || strafe != 0) {
                float yRot = player.getYRot() * Mth.DEG_TO_RAD;
                float sin = Mth.sin(yRot);
                float cos = Mth.cos(yRot);
                double dx = strafe * cos - forward * sin;
                double dz = forward * cos + strafe * sin;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.001) {
                    dirX = (float) (dx / len);
                    dirZ = (float) (dz / len);
                }
            }
        }
        NetworkHandler.INSTANCE.sendToServer(new AbilityUsePacket(slot, dirX, dirZ));
    }

    public static void startCharging(String slot) {
        chargingSlot = slot;
        chargeTicks = 0;
    }

    public static int getChargeTicks() {
        return chargeTicks;
    }

    public static String getChargingSlot() {
        return chargingSlot;
    }
}
