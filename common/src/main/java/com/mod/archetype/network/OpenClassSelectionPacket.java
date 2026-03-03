package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;

public class OpenClassSelectionPacket {

    public static final byte MODE_FIRST_SELECT = 0;
    public static final byte MODE_REBORN = 1;

    private final byte mode;

    public OpenClassSelectionPacket(byte mode) {
        this.mode = mode;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(mode);
    }

    public static OpenClassSelectionPacket decode(FriendlyByteBuf buf) {
        return new OpenClassSelectionPacket(buf.readByte());
    }

    public byte getMode() {
        return mode;
    }

    public boolean isReborn() {
        return mode == MODE_REBORN;
    }
}
