package com.mod.archetype.network.client;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientOtherPlayersData {

    private static final Map<UUID, ResourceLocation> playerClasses = new ConcurrentHashMap<>();

    public static void update(UUID playerId, @Nullable ResourceLocation classId) {
        if (classId != null) {
            playerClasses.put(playerId, classId);
        } else {
            playerClasses.remove(playerId);
        }
    }

    @Nullable
    public static ResourceLocation getClass(UUID playerId) {
        return playerClasses.get(playerId);
    }

    public static void remove(UUID playerId) {
        playerClasses.remove(playerId);
    }

    public static void clear() {
        playerClasses.clear();
    }
}
