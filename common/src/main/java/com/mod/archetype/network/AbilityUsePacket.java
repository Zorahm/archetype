package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;

public class AbilityUsePacket {

    private final String slotName;
    private final float moveDirX;
    private final float moveDirZ;

    public AbilityUsePacket(String slotName) {
        this(slotName, 0, 0);
    }

    public AbilityUsePacket(String slotName, float moveDirX, float moveDirZ) {
        this.slotName = slotName;
        this.moveDirX = moveDirX;
        this.moveDirZ = moveDirZ;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(slotName, 32);
        buf.writeFloat(moveDirX);
        buf.writeFloat(moveDirZ);
    }

    public static AbilityUsePacket decode(FriendlyByteBuf buf) {
        String slot = buf.readUtf(32);
        float dirX = buf.readFloat();
        float dirZ = buf.readFloat();
        return new AbilityUsePacket(slot, dirX, dirZ);
    }

    public String getSlotName() {
        return slotName;
    }

    public float getMoveDirX() {
        return moveDirX;
    }

    public float getMoveDirZ() {
        return moveDirZ;
    }
}
