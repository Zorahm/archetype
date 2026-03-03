package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public class PlayerClassSyncPacket {

    private final UUID playerUUID;
    private final boolean hasClass;
    @Nullable
    private final ResourceLocation classId;

    public PlayerClassSyncPacket(UUID playerUUID, boolean hasClass, @Nullable ResourceLocation classId) {
        this.playerUUID = playerUUID;
        this.hasClass = hasClass;
        this.classId = classId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(hasClass);
        if (hasClass && classId != null) {
            buf.writeResourceLocation(classId);
        }
    }

    public static PlayerClassSyncPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        boolean hasClass = buf.readBoolean();
        ResourceLocation classId = null;
        if (hasClass) {
            classId = buf.readResourceLocation();
        }
        return new PlayerClassSyncPacket(uuid, hasClass, classId);
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean hasClass() { return hasClass; }
    @Nullable public ResourceLocation getClassId() { return classId; }
}
