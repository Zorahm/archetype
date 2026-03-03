package com.mod.archetype.network;

import com.mod.archetype.Archetype;
import com.mod.archetype.network.handler.*;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.resources.ResourceLocation;

public class NetworkInit {

    public static void register(NetworkHandler handler) {
        // Client -> Server
        handler.registerServerReceiver(
                id("class_select"), ClassSelectPacket.class,
                ClassSelectPacket::decode, ClassSelectHandler::handle
        );

        handler.registerServerReceiver(
                id("ability_use"), AbilityUsePacket.class,
                AbilityUsePacket::decode, AbilityUseHandler::handle
        );

        handler.registerServerReceiver(
                id("ability_release"), AbilityReleasePacket.class,
                AbilityReleasePacket::decode, AbilityReleaseHandler::handle
        );

        // Server -> Client
        handler.registerClientReceiver(
                id("open_selection"), OpenClassSelectionPacket.class,
                OpenClassSelectionPacket::decode, OpenSelectionClientHandler::handle
        );

        handler.registerClientReceiver(
                id("sync_data"), SyncClassDataPacket.class,
                SyncClassDataPacket::decode, SyncDataClientHandler::handle
        );

        handler.registerClientReceiver(
                id("assign_result"), ClassAssignResultPacket.class,
                ClassAssignResultPacket::decode, AssignResultClientHandler::handle
        );

        handler.registerClientReceiver(
                id("player_class_sync"), PlayerClassSyncPacket.class,
                PlayerClassSyncPacket::decode, PlayerClassSyncClientHandler::handle
        );
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Archetype.MOD_ID, path);
    }
}
