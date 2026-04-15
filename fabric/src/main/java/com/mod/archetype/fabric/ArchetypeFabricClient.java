package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import com.mod.archetype.config.ConfigManager;
import com.mod.archetype.gui.AbilityHudOverlay;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import com.mod.archetype.platform.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ArchetypeFabricClient implements ClientModInitializer {

    private static KeyMapping ABILITY_1_KEY;
    private static KeyMapping ABILITY_2_KEY;
    private static KeyMapping ABILITY_3_KEY;
    private static KeyMapping CLASS_INFO_KEY;

    @Override
    public void onInitializeClient() {
        Archetype.initClient();
        ConfigManager.loadClientConfig(FabricLoader.getInstance().getConfigDir());

        if (NetworkHandler.INSTANCE instanceof FabricNetworkHandler fnh) {
            fnh.initClient();
        }

        // Register keybindings
        KeyMapping.Category archetypeCategory = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "keybinds"));
        ABILITY_1_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.ABILITY_1, GLFW.GLFW_KEY_R, archetypeCategory));
        ABILITY_2_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.ABILITY_2, GLFW.GLFW_KEY_V, archetypeCategory));
        ABILITY_3_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.ABILITY_3, GLFW.GLFW_KEY_G, archetypeCategory));
        CLASS_INFO_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.CLASS_INFO, GLFW.GLFW_KEY_J, archetypeCategory));

        // HUD overlay
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            AbilityHudOverlay.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        });

        ArchetypeKeybinds.setKeyMappings(ABILITY_1_KEY, ABILITY_2_KEY, ABILITY_3_KEY, CLASS_INFO_KEY);

        // Client tick: keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ArchetypeKeybinds.tickKeybinds(ABILITY_1_KEY, ABILITY_2_KEY, ABILITY_3_KEY, CLASS_INFO_KEY);
        });
    }
}
