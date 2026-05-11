package com.mod.archetype.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArchetypeWorldData extends SavedData {

    private static final String DATA_NAME = "archetype_class_data";
    private final Map<UUID, CompoundTag> playerData = new HashMap<>();

    public static final Codec<ArchetypeWorldData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, CompoundTag.CODEC)
                            .fieldOf("Players")
                            .forGetter(data -> data.playerData)
            ).apply(instance, playerMap -> {
                ArchetypeWorldData data = new ArchetypeWorldData();
                data.playerData.putAll(playerMap);
                return data;
            })
    );

    public static final SavedDataType<ArchetypeWorldData> TYPE = new SavedDataType<>(
            DATA_NAME, ArchetypeWorldData::new, CODEC, DataFixTypes.LEVEL
    );

    public static ArchetypeWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public CompoundTag getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }

    public void setPlayerData(UUID uuid, CompoundTag tag) {
        playerData.put(uuid, tag);
        setDirty();
    }
}
