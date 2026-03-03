package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import net.fabricmc.api.ClientModInitializer;

public class ArchetypeFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Archetype.initClient();
    }
}
