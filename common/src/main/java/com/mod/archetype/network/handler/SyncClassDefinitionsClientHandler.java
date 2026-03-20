package com.mod.archetype.network.handler;

import com.mod.archetype.network.SyncClassDefinitionsPacket;
import com.mod.archetype.registry.ClassRegistry;

public class SyncClassDefinitionsClientHandler {

    public static void handle(SyncClassDefinitionsPacket packet) {
        ClassRegistry.getInstance().loadFromJsonStrings(packet.getClassJsonMap());
    }
}
