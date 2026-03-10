package com.mod.archetype.network.handler;

import com.mod.archetype.Archetype;
import com.mod.archetype.network.OpenClassSelectionPacket;
import net.minecraft.client.Minecraft;

public class OpenSelectionClientHandler {

    public static void handle(OpenClassSelectionPacket packet) {
        Minecraft.getInstance().execute(() -> {
            Archetype.LOGGER.debug("Opening class selection screen, mode={}",
                    packet.isReborn() ? "reborn" : "first_select");
            Minecraft.getInstance().setScreen(
                    new com.mod.archetype.gui.ClassSelectionScreen(packet.isReborn() ? 1 : 0));
        });
    }
}
