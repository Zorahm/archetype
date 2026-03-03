package com.mod.archetype.fabric;

import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric implementation of PlayerDataAccess.
 * Uses a simple map-based approach. In production, this should use
 * Cardinal Components API or Fabric Data Attachment API for proper
 * persistence and synchronization.
 */
public class FabricPlayerDataAccess implements PlayerDataAccess {

    private static final Map<UUID, PlayerClassData> PLAYER_DATA = new ConcurrentHashMap<>();

    @Override
    public PlayerClassData getClassData(Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), uuid -> new PlayerClassData());
    }

    @Override
    public void setClassData(Player player, PlayerClassData data) {
        PLAYER_DATA.put(player.getUUID(), data);
    }

    public static void onPlayerJoin(ServerPlayer player, CompoundTag savedData) {
        PlayerClassData data = new PlayerClassData();
        if (savedData != null && savedData.contains("archetype")) {
            data.load(savedData.getCompound("archetype"));
        }
        PLAYER_DATA.put(player.getUUID(), data);
    }

    public static CompoundTag onPlayerSave(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        PlayerClassData data = PLAYER_DATA.get(player.getUUID());
        if (data != null) {
            tag.put("archetype", data.save());
        }
        return tag;
    }

    public static void onPlayerLeave(ServerPlayer player) {
        PLAYER_DATA.remove(player.getUUID());
    }

    public static void copyData(ServerPlayer original, ServerPlayer newPlayer) {
        PlayerClassData oldData = PLAYER_DATA.get(original.getUUID());
        if (oldData != null) {
            PlayerClassData newData = new PlayerClassData();
            newData.load(oldData.save());
            PLAYER_DATA.put(newPlayer.getUUID(), newData);
        }
    }
}
