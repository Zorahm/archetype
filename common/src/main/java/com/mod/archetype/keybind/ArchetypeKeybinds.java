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

    private static KeyMapping ability1Key;
    private static KeyMapping ability2Key;
    private static KeyMapping ability3Key;
    private static KeyMapping classInfoKey;

    private static String chargingSlot = null;
    private static int chargeTicks = 0;

    public static void setKeyMappings(KeyMapping a1, KeyMapping a2, KeyMapping a3, KeyMapping ci) {
        ability1Key = a1;
        ability2Key = a2;
        ability3Key = a3;
        classInfoKey = ci;
    }

    /** Full translated key name for a slot (0=ability_1, 1=ability_2, 2=ability_3). */
    public static String getSlotKeyDisplay(int slot) {
        KeyMapping key = switch (slot) {
            case 0 -> ability1Key;
            case 1 -> ability2Key;
            case 2 -> ability3Key;
            default -> null;
        };
        if (key == null) return "?";
        return key.getTranslatedKeyMessage().getString();
    }

    /** Abbreviated key name for compact HUD display (max 3 chars). */
    public static String getSlotKeyDisplayShort(int slot) {
        String name = getSlotKeyDisplay(slot);
        return name.length() > 3 ? name.substring(0, 3) : name;
    }

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
            net.minecraft.world.phys.Vec2 moveVec = player.input.getMoveVector();
            float forward = moveVec.y;
            float strafe = moveVec.x;
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
