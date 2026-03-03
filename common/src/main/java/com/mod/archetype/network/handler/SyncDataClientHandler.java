package com.mod.archetype.network.handler;

import com.mod.archetype.network.SyncClassDataPacket;
import com.mod.archetype.network.client.ClientClassData;
import net.minecraft.client.Minecraft;

public class SyncDataClientHandler {

    public static void handle(SyncClassDataPacket packet) {
        Minecraft.getInstance().execute(() -> {
            ClientClassData.getInstance().update(packet);
        });
    }
}
