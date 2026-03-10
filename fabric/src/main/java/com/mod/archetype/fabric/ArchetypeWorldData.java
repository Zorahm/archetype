package com.mod.archetype.fabric;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArchetypeWorldData extends SavedData {

    private static final String DATA_NAME = "archetype_class_data";
    private final Map<UUID, CompoundTag> playerData = new HashMap<>();

    public static ArchetypeWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ArchetypeWorldData::load, ArchetypeWorldData::new, DATA_NAME);
    }

    @Nullable
    public CompoundTag getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }

    public void setPlayerData(UUID uuid, CompoundTag tag) {
        playerData.put(uuid, tag);
        setDirty();
    }

    public static ArchetypeWorldData load(CompoundTag tag) {
        ArchetypeWorldData data = new ArchetypeWorldData();
        CompoundTag players = tag.getCompound("Players");
        for (String key : players.getAllKeys()) {
            try {
                data.playerData.put(UUID.fromString(key), players.getCompound(key));
            } catch (Exception ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        playerData.forEach((uuid, data) -> players.put(uuid.toString(), data));
        tag.put("Players", players);
        return tag;
    }
}
