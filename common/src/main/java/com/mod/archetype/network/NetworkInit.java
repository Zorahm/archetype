package com.mod.archetype.network;

import com.mod.archetype.Archetype;
import com.mod.archetype.network.handler.*;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.resources.Identifier;

public class NetworkInit {

    public static void register(NetworkHandler handler) {
        // Client -> Server
        handler.registerServerReceiver(
                id("class_select"), ClassSelectPacket.class,
                ClassSelectPacket::encode, ClassSelectPacket::decode, ClassSelectHandler::handle
        );

        handler.registerServerReceiver(
                id("ability_use"), AbilityUsePacket.class,
                AbilityUsePacket::encode, AbilityUsePacket::decode, AbilityUseHandler::handle
        );

        handler.registerServerReceiver(
                id("ability_release"), AbilityReleasePacket.class,
                AbilityReleasePacket::encode, AbilityReleasePacket::decode, AbilityReleaseHandler::handle
        );

        // Server -> Client
        handler.registerClientReceiver(
                id("open_selection"), OpenClassSelectionPacket.class,
                OpenClassSelectionPacket::encode, OpenClassSelectionPacket::decode, OpenSelectionClientHandler::handle
        );

        handler.registerClientReceiver(
                id("sync_data"), SyncClassDataPacket.class,
                SyncClassDataPacket::encode, SyncClassDataPacket::decode, SyncDataClientHandler::handle
        );

        handler.registerClientReceiver(
                id("assign_result"), ClassAssignResultPacket.class,
                ClassAssignResultPacket::encode, ClassAssignResultPacket::decode, AssignResultClientHandler::handle
        );

        handler.registerClientReceiver(
                id("player_class_sync"), PlayerClassSyncPacket.class,
                PlayerClassSyncPacket::encode, PlayerClassSyncPacket::decode, PlayerClassSyncClientHandler::handle
        );

        handler.registerClientReceiver(
                id("sync_class_defs"), SyncClassDefinitionsPacket.class,
                SyncClassDefinitionsPacket::encode, SyncClassDefinitionsPacket::decode, SyncClassDefinitionsClientHandler::handle
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, path);
    }
}
