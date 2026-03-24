package com.mod.archetype.forge;

import com.mod.archetype.Archetype;
import com.mod.archetype.config.ConfigManager;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

@Mod(Archetype.MOD_ID)
public class ArchetypeForge {

    public static KeyMapping ABILITY_1_KEY;
    public static KeyMapping ABILITY_2_KEY;
    public static KeyMapping ABILITY_3_KEY;
    public static KeyMapping CLASS_INFO_KEY;

    public ArchetypeForge() {
        Archetype.init();
        MinecraftForge.EVENT_BUS.register(ForgeEventTranslator.class);

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::registerKeyMappings);
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ConfigManager.loadServerConfig(FMLPaths.CONFIGDIR.get());
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        Archetype.initClient();
        ConfigManager.loadClientConfig(FMLPaths.CONFIGDIR.get());
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ABILITY_1_KEY = new KeyMapping(ArchetypeKeybinds.ABILITY_1, GLFW.GLFW_KEY_R, ArchetypeKeybinds.CATEGORY);
        ABILITY_2_KEY = new KeyMapping(ArchetypeKeybinds.ABILITY_2, GLFW.GLFW_KEY_V, ArchetypeKeybinds.CATEGORY);
        ABILITY_3_KEY = new KeyMapping(ArchetypeKeybinds.ABILITY_3, GLFW.GLFW_KEY_G, ArchetypeKeybinds.CATEGORY);
        CLASS_INFO_KEY = new KeyMapping(ArchetypeKeybinds.CLASS_INFO, GLFW.GLFW_KEY_J, ArchetypeKeybinds.CATEGORY);
        event.register(ABILITY_1_KEY);
        event.register(ABILITY_2_KEY);
        event.register(ABILITY_3_KEY);
        event.register(CLASS_INFO_KEY);
        ArchetypeKeybinds.setKeyMappings(ABILITY_1_KEY, ABILITY_2_KEY, ABILITY_3_KEY, CLASS_INFO_KEY);
    }
}
