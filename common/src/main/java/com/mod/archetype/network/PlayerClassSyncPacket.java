package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class PlayerClassSyncPacket {

    private final UUID playerUUID;
    private final boolean hasClass;
    @Nullable
    private final Identifier classId;

    public PlayerClassSyncPacket(UUID playerUUID, boolean hasClass, @Nullable Identifier classId) {
        this.playerUUID = playerUUID;
        this.hasClass = hasClass;
        this.classId = classId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(hasClass);
        if (hasClass && classId != null) {
            buf.writeIdentifier(classId);
        }
    }

    public static PlayerClassSyncPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        boolean hasClass = buf.readBoolean();
        Identifier classId = null;
        if (hasClass) {
            classId = buf.readIdentifier();
        }
        return new PlayerClassSyncPacket(uuid, hasClass, classId);
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean hasClass() { return hasClass; }
    @Nullable public Identifier getClassId() { return classId; }
}
