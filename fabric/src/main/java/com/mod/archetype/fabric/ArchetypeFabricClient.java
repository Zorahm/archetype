package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import com.mod.archetype.config.ConfigManager;
import com.mod.archetype.gui.AbilityHudOverlay;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
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

        // Register keybindings
        ABILITY_1_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.ABILITY_1, GLFW.GLFW_KEY_R, ArchetypeKeybinds.CATEGORY));
        ABILITY_2_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.ABILITY_2, GLFW.GLFW_KEY_V, ArchetypeKeybinds.CATEGORY));
        ABILITY_3_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.ABILITY_3, GLFW.GLFW_KEY_G, ArchetypeKeybinds.CATEGORY));
        CLASS_INFO_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(ArchetypeKeybinds.CLASS_INFO, GLFW.GLFW_KEY_J, ArchetypeKeybinds.CATEGORY));

        // HUD overlay
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            AbilityHudOverlay.render(graphics, tickDelta);
        });

        // Client tick for keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ArchetypeKeybinds.tickKeybinds(ABILITY_1_KEY, ABILITY_2_KEY, ABILITY_3_KEY, CLASS_INFO_KEY);
        });
    }
}
