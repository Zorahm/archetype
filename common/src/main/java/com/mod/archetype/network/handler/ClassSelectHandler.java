package com.mod.archetype.network.handler;

import com.mod.archetype.Archetype;
import com.mod.archetype.core.ArchetypeEvents;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.ClassAssignResultPacket;
import com.mod.archetype.network.ClassSelectPacket;
import com.mod.archetype.network.PlayerClassSyncPacket;
import com.mod.archetype.platform.NetworkHandler;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ClassSelectHandler {

    public static void handle(ServerPlayer player, ClassSelectPacket packet) {
        Archetype.LOGGER.trace("Received ClassSelectPacket from {}: classId={}, viaItem={}",
                player.getName().getString(), packet.getClassId(), packet.isViaItem());

        ResourceLocation classId = packet.getClassId();

        // Validation 1: classId sanity
        if (classId == null || classId.toString().length() > 256) {
            Archetype.LOGGER.warn("Player {} sent invalid class select packet", player.getName().getString());
            sendFailure(player, "archetype.error.invalid_packet");
            return;
        }

        // Validation 2: class exists
        PlayerClass classDef = ClassManager.getInstance().getClassDefinition(classId);
        if (classDef == null) {
            Archetype.LOGGER.warn("Player {} tried to select non-existent class: {}", player.getName().getString(), classId);
            sendFailure(player, "archetype.error.class_not_found");
            return;
        }

        // Validation 3: class not disabled (TODO: check config disabledClasses)

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);

        // Validation 4: change cooldown
        // TODO: check classChangeCooldownTicks from config

        // Validation 5: via item check
        if (packet.isViaItem()) {
            // TODO: check if MainHand contains Scroll of Rebirth
        }

        // Validation 6: incompatible classes
        if (data.hasClass()) {
            ResourceLocation currentClassId = data.getCurrentClassId();
            if (classDef.getIncompatibleWith().contains(currentClassId)) {
                Archetype.LOGGER.warn("Player {} tried to switch to incompatible class: {} -> {}",
                        player.getName().getString(), currentClassId, classId);
                sendFailure(player, "archetype.error.incompatible_class");
                return;
            }
        }

        // Assign the class
        ClassManager.AssignResult result = ClassManager.getInstance().assignClass(player, classId);

        if (result.success()) {
            // Consume item if via item
            if (packet.isViaItem()) {
                player.getMainHandItem().shrink(1);
            }

            // Send success to the player
            NetworkHandler.INSTANCE.sendToPlayer(player,
                    new ClassAssignResultPacket(true, null));

            // Notify other players
            NetworkHandler.INSTANCE.sendToTracking(player,
                    new PlayerClassSyncPacket(player.getUUID(), true, classId));
        } else {
            Archetype.LOGGER.warn("Player {} failed class select {}: {}",
                    player.getName().getString(), classId, result.failReasonKey());
            sendFailure(player, result.failReasonKey());
        }
    }

    private static void sendFailure(ServerPlayer player, String reasonKey) {
        NetworkHandler.INSTANCE.sendToPlayer(player,
                new ClassAssignResultPacket(false, reasonKey));
    }
}
