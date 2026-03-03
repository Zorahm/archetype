package com.mod.archetype.network.handler;

import com.mod.archetype.Archetype;
import com.mod.archetype.network.ClassAssignResultPacket;
import net.minecraft.client.Minecraft;

public class AssignResultClientHandler {

    public static void handle(ClassAssignResultPacket packet) {
        Minecraft.getInstance().execute(() -> {
            if (packet.isSuccess()) {
                Archetype.LOGGER.debug("Class assignment succeeded");
                // Close selection screen
                if (Minecraft.getInstance().screen != null) {
                    Minecraft.getInstance().setScreen(null);
                }
                // TODO: show toast notification, play sound
            } else {
                Archetype.LOGGER.debug("Class assignment failed: {}", packet.getFailReasonKey());
                // TODO: show error message on selection screen
            }
        });
    }
}
