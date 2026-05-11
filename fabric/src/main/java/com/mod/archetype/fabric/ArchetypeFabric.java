package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import com.mod.archetype.config.ConfigManager;
import com.mod.archetype.item.ModItems;
import com.mod.archetype.item.RebirthScrollItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class ArchetypeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Archetype.init();
        ConfigManager.loadServerConfig(FabricLoader.getInstance().getConfigDir());

        // Register items
        Identifier rebirthScrollId = Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "rebirth_scroll");
        ResourceKey<Item> rebirthScrollKey = ResourceKey.create(Registries.ITEM, rebirthScrollId);
        ModItems.REBIRTH_SCROLL = Registry.register(
                BuiltInRegistries.ITEM,
                rebirthScrollKey,
                new RebirthScrollItem(new Item.Properties().setId(rebirthScrollKey).stacksTo(1))
        );

        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(ModItems.REBIRTH_SCROLL);
        });

        FabricEventTranslator.register();
    }
}
