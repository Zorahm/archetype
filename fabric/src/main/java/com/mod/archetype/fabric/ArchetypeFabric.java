package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import net.fabricmc.api.ModInitializer;

public class ArchetypeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Archetype.init();
        FabricEventTranslator.register();
    }
}
