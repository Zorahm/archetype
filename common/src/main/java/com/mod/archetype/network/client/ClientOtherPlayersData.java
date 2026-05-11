package com.mod.archetype.network.client;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientOtherPlayersData {

    private static final Map<UUID, Identifier> playerClasses = new ConcurrentHashMap<>();

    public static void update(UUID playerId, @Nullable Identifier classId) {
        if (classId != null) {
            playerClasses.put(playerId, classId);
        } else {
            playerClasses.remove(playerId);
        }
    }

    @Nullable
    public static Identifier getClass(UUID playerId) {
        return playerClasses.get(playerId);
    }

    public static void remove(UUID playerId) {
        playerClasses.remove(playerId);
    }

    public static void clear() {
        playerClasses.clear();
    }
}
