package com.mod.archetype.network.handler;

import com.mod.archetype.network.PlayerClassSyncPacket;
import com.mod.archetype.network.client.ClientOtherPlayersData;
import net.minecraft.client.Minecraft;

public class PlayerClassSyncClientHandler {

    public static void handle(PlayerClassSyncPacket packet) {
        Minecraft.getInstance().execute(() -> {
            if (packet.hasClass()) {
                ClientOtherPlayersData.update(packet.getPlayerUUID(), packet.getClassId());
            } else {
                ClientOtherPlayersData.remove(packet.getPlayerUUID());
            }
        });
    }
}
